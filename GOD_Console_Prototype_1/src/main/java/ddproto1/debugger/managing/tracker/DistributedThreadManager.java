/*
 * Created on Sep 23, 2004
 *
 */
package ddproto1.debugger.managing.tracker;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;


import com.sun.jdi.request.EventRequest;

import ddproto1.util.MessageHandler;
import ddproto1.util.commons.ByteMessage;
import ddproto1.util.commons.Event;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.ILocalThreadManager;
import ddproto1.debugger.managing.ILocalNodeManager;
import ddproto1.debugger.managing.IThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread.VirtualStack;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.DeferrableHookRequest;
import ddproto1.debugger.server.IRequestHandler;
import ddproto1.exception.DistributedStackOverflowException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoContextException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.PropertyViolation;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.ParserException;
import ddproto1.util.collection.LockingHashMap;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * The <b>Distributed Thread Manager</b> is a critical component, 
 * responsible for providing access to system-wide distributed threads 
 * as well as for collecting update information from out-of-band (DDWP)
 * and in-band (JDWP) event dispatchers and merging them into an approximation
 * of the distributed systems state (toghether with other components).
 * 
 * NOTE: The current implementation can be brutally optimized. It is also broken
 * with respect to memory synchronization.
 * 
 * Note about this class: I'm getting more and more convinced that this class should
 * be internal to distributed thread, that each local thread should have an associated
 * "default" distributed thread to which it is associated when it is not associated 
 * to other distributed threads, and that the CLIENT_UPCALL stage is useless. 
 * 
 * @author giuliano
 *
 */
public class DistributedThreadManager implements IRequestHandler, IThreadManager {
    
    private static final int MAXIMUM_STACK_SIZE = 99;
    private static final Logger logger = MessageHandler.getInstance().getLogger(DistributedThreadManager.class);
    private static final Logger modifiersLogger = MessageHandler.getInstance().getLogger(DistributedThreadManager.class.getName() + ".modifiersLogger");
    private static final Logger stackLogger = MessageHandler.getInstance().getLogger(DistributedThreadManager.class.getName() + ".stackLogger");
    
    private static ConversionUtil fmh = ConversionUtil.getInstance();
   
    /** This flag is for debugging purposes.
     * When in stack builder mode, the tracker simply builds the 
     * distributed stack without concerning itself with breakpoints 
     * or any VMM related stuff. In fact, it avoids acessing the
     * VMMs altogether. This allows us to run both the debugger
     * and the debuggees under another debugger and follow their flow.
     */
    private final boolean stackBuilderMode = false;
    
    private LockingHashMap <Integer, DistributedThread> dthreads;
    private LockingHashMap <Integer, Integer> threads2dthreads;
    private IDebugTarget parent;

    public DistributedThreadManager(IDebugTarget parent){ 
        dthreads = new LockingHashMap<Integer, DistributedThread>();
        threads2dthreads = new LockingHashMap<Integer, Integer>(); 
        this.parent = parent;
    }
    
    public IThread getThread(Integer uuid) 
    		throws NoContextException {
    			return getByUUID(new Integer(uuid));
    }
    
    public DistributedThread getByUUID(Integer uuid)
    	throws NoContextException
    {
        try{
            dthreads.lockForReading();
            DistributedThread dt = dthreads.get(uuid);
            if(dt == null)
                throw new NoContextException("Required distributed thread doesn't exist.");
            return dt;
        }finally{
            dthreads.unlockForReading();
        }
    }
    
    public Integer getEnclosingDT(Integer dtId){
        try{
            threads2dthreads.lockForReading();
            return threads2dthreads.get(dtId);
        }finally{
            threads2dthreads.unlockForReading();
        }
    }
    
    public boolean existsDT(Integer dtId) {
        try {
            dthreads.lockForReading();
            return dthreads.containsKey(dtId);
        } finally {
            dthreads.unlockForReading();
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.configurator.IRequestHandler#handleRequest(java.lang.Byte, ddproto1.util.ByteMessage)
     */
    public ByteMessage handleRequest(Byte gid, ByteMessage req) 
    {
        /**
         * Note: I'm almost certain that I could only earn cohesion by moving
         * event processing code to the distributed thread themselves, since
         * their event processing is completely independent. 
         * 
         */
        ByteMessage ret = null;
        byte mode;
        
        // This is the last coupling between java and DTM. Removing this requires
        // changing the way the debugger gives access to the node manager registries.
        // An extra level of indirection would surely suffice.
        ILocalNodeManager vmm = VMManagerFactory.getRegistryManagerInstance().getNodeManager(gid);
        assert vmm != null;
        
        try {
            
            Event evt = new Event(req.getMessage());

            switch (evt.getType()) {

            /**
             * Stage 1 - Client upcall. Client made a call to a CORBA object, which may or may not
             * be a remote object. Unfortunately we don't know that yet. This stage basically promotes
             * the local thread to distributed. We have this "promotion" because even though we could
             * treat all threads as being distributed - local threads would then just be distributed threads
             * whose component set is be empty - we don't. Our representation is non-uniform (thanks to 
             * myself and JDI) and therefore we have to switch between representations in order to achieve
             * what we must. This representation switching is the "promotion". 
             * 
             * REMARK Consider eliminating this stage alltogether. It seems that a lot of complications
             *        arise because of it. I'm getting convinced we could stuff this all at the server
             *        side (SERVER_RECEIVE) without further problems.
             */
            case DebuggerConstants.CLIENT_UPCALL:
            {
                /* Obtains the class name and operation to
            	 * which the distributed thread is addressed to.
            	 */
                String dtuid = evt.getAttribute("dtid");
                String ltuid = evt.getAttribute("ltid");
                String op = evt.getAttribute("op");
                String top = evt.getAttribute("top");

                /* Id's go in hex format because it's more economical */
            	int dt_uuid = (int)fmh.hex2Int(dtuid);
            	int lt_uuid = (int)fmh.hex2Int(ltuid);
            	int st_size = (int)fmh.hex2Int(top);
            	
            	Integer dt_uuid_wrap = new Integer(dt_uuid);
            	Integer lt_uuid_wrap = new Integer(lt_uuid);
            	Integer st_size_wrap = new Integer(st_size);
            	
            	DistributedThread current = null;
            	
            	/* Thread promotion occurs here (local thread becomes
                 * distributed thread).
                 */
                boolean unlocked = true;
            	try {
                    /*
                     * Only starts playing around with objects if we can
                     * provide atomic modifications to table readers.
                     */
            	    lockAllTables();
                    unlocked = false;

                    VirtualStackframe vsf;
                    
                    ILocalThread ilt = null;

                    if (!dthreads.containsKey(dt_uuid_wrap)) {
                        /*
                         * If thread hasn't been promoted then it must be part
                         * of itself (it's a thread root)
                         */
                        assert (ltuid.equals(dtuid));
                        /*
                         * Creates the distributed thread root (should be done
                         * once per thread).
                         */
                        if(!stackBuilderMode){
                            ILocalThreadManager tm = vmm.getThreadManager();
                            ilt = tm.getLocalThread(lt_uuid_wrap);
                            vsf = new VirtualStackframe(op, null, ilt, parent);
                            if(ilt == null)
                                vsf.flagAsDamaged("Thread being pushed doesn't exist.");

                            current = new DistributedThread(vsf, this, parent);
                        }else{
                            vsf = new VirtualStackframe(op, null, null, parent);
                            current = new DistributedThread(vsf, null, null);
                        }
                        
                        current.lock();
                        dthreads.put(dt_uuid_wrap, current);

                        /* Updates the local-to-distributed thread index */
                        threads2dthreads.put(lt_uuid_wrap, dt_uuid_wrap);
                        
                        vsf.setCallTop(st_size_wrap);

                        unlockAllTables();
                        unlocked = true;

                        current.fireCreationEvent();

                        if(stackBuilderMode) break;
                    }
                    
                    else{
                    	current = (DistributedThread) dthreads
								.get(dt_uuid_wrap);
                    	vsf = current.virtualStack().peek();
                        assert(vsf != null);
                    	current.lock();
                        vsf.setCallTop(st_size_wrap);
                        vsf.setOutboundOperation(op);
                        ilt = vsf.getThreadReference();
                        
                        /** Checks to see if there haven't been any
                         * inconsistencies.
                         */
                        if(!ilt.getGUID().equals(lt_uuid)){
                            logger.error("Upcall thread doesn't match " +
                                    "stack top." + 
                                    "Top thread is: " + fmh.uuid2Dotted(ilt.getGUID()) +
                                    "When it should be: " + fmh.uuid2Dotted(lt_uuid) +
                                    "Flagging Distributed Thread as damaged as it can no longer provide " +
                                    "reliable information.");
                            current.flagAsDamaged();
                        }
                    }
                    
                    /** Apply state modifiers, if any. */
                    if(!stackBuilderMode){
                        assert ilt != null;
                        if(modifiersLogger.isDebugEnabled()){
                            ConversionUtil cUtil = ConversionUtil.getInstance();
                            modifiersLogger.debug("Processing upcall modifiers for local thread ("
                                    + cUtil.uuid2Dotted(ilt.getGUID())
                                    + ")\nwho belongs to machine <"
                                    + ilt.getDebugTarget().getName()
                                    + ">\nand has hash code " + ilt.hashCode());
                        }
                        if(ilt.resumedByRemoteStepping()){
                            current.beginRemoteStepping(DebugEvent.STEP_INTO);
                        }
                    }
                    
                    if(stackLogger.isDebugEnabled()){
                        stackLogger.debug("Upcall processed: "
                                + "\n Operation name: " + op 
                                + "\n Distributed thread: " + fmh.uuid2Dotted((int)dt_uuid)
                                + "\n Client-side thread: " + fmh.uuid2Dotted((int)lt_uuid));
                    }
                        
                    mode = current.getMode();
                    
            	} finally {
                    if(!unlocked) unlockAllTables();
                    if(current != null)
                        current.unlock();
                }
                
                break;
            }
            
            case DebuggerConstants.SERVER_RECEIVE:
            {
                /* Obtain data from message */
                String op = evt.getAttribute("op");
            	String dtuid = evt.getAttribute("dtid");
            	String ltuid = evt.getAttribute("ltid");
            	String fullOp = evt.getAttribute("fop");
            	String base = evt.getAttribute("siz");
               
                boolean merge = false;
            	
            	/* Decodes and stores ids for usage during this request */
            	long dt_uuid = (int)fmh.hex2Long(dtuid);
            	long lt_uuid = (int)fmh.hex2Long(ltuid);
            	
            	assert(((int)dt_uuid) == dt_uuid);
            	assert(((int)lt_uuid) == lt_uuid);
            	
            	Integer dt_uuid_wrap = new Integer((int)dt_uuid);
            	Integer lt_uuid_wrap = new Integer((int)lt_uuid);
            	DistributedThread current = null;

            	/* ANNOYANCE - Those try-finally blocks are really annoying,
            	 * but we can't condense them since the readwrite locks we 
            	 * use for the tables will throw an IllegalStateException if
            	 * we attempt to realease them twice - this effectivelly forces
            	 * us to release the lock ONLY at the finally block (and therefore
            	 * to nest finally blocks if we want to release any locks earlier
            	 * than the others).
            	 */
            	try {
                    try {
                        lockAllTables();
                        /*
                         * Obtains a reference to the Distributed Thread labeled
                         * dt_uuid
                         */
                        current = (DistributedThread) dthreads
                                .get(dt_uuid_wrap);
                        /*
                         * Should exist (hypothesis is that if this call has
                         * reached this far than it must have passed through a
                         * client upcall section)
                         */
                        assert (current != null);
                        
                        /* When a the server-side local thread is already part of a 
                         * distributed thread, one of the following should hold:
                         * 
                         * 1 - This thread was promoted to distributed in the past and it's
                         *     a root of a distributed thread whose virtual stack is empty 
                         *     (with the exception of the root itself). In this case, the only
                         *     reason why the local thread is being reported as part of a DT 
                         *     is because the root thread *is* actually the distributed thread, so 
                         *     it's permanently part of itself (though this becomes apparent to 
                         *     the debugger only after promotion occurs). This is OK.
                         *     
                         * 2 - This is not actually a remote call. Some ORBs run the interceptors
                         *     even when the call is not remote. Since we cannot predict if the call 
                         *     is actually a remote call at the CLIENT_UPCALL interception point, 
                         *     we must detect it here. In this case, we do a "frame merge". This is 
                         *     OK.
                         * 
                         * 3 - Something actually went wrong and two active calls are reporting as
                         *     being served by the same local thread, at the same time. This is bad. 
                         * 
                         */
                        if(threads2dthreads.containsKey(lt_uuid_wrap)){
                            
                            DistributedThread enclosing = (DistributedThread)dthreads.get(threads2dthreads.get(lt_uuid_wrap));
                            enclosing.lock();
                            DistributedThread.VirtualStack vs = enclosing.virtualStack();
                                                        
                            VirtualStackframe last = vs.peek();
                            
                            /* It's a remote call if and only if the thread making the call is different
                             * from the thread servicing it. */
                            if (!last.getLocalThreadId().equals(lt_uuid_wrap)) {
                                
                                /* If we're in case 1, two properties will have to hold. 
                                 * First - the local thread is a root.
                                 * Second - the virtual stackframe should be empty (with the exception
                                 *          of the root, of course) 
                                 */
                                if (enclosing.getId() != lt_uuid || vs.getVirtualFrameCount() > 1){
                                    /* Something went wrong, we're in case 3. */
                                    throw new PropertyViolation(
                                            "A local thread cannot participate in more "
                                                    + "than one distributed thread at the same time.");
                                }

                                /*
                                 * Otherwise we just have to reallocate - 
                                 * toss away the distributed thread and it'll be
                                 * pushed into some other thread's call stack.
                                 * OPTIMIZATION Use a pool of DistributedThread
                                 * instances to minimize the effect of creating
                                 * instances non-stop.
                                 */
                                dthreads.remove(lt_uuid_wrap);
                                assert (threads2dthreads
                                        .containsKey(lt_uuid_wrap));
                                threads2dthreads.remove(lt_uuid_wrap);
                            }
                            
                            /* It's not a remote call, we're in case 2. Merge the frames. */
                            else {
                                merge = true;
                            }
                        }

                        if(!merge){
                            /*
                             * Pushes the new virtual stack frame that maps to the
                             * stack of the local thread that services the request
                             * at the client-side
                             */
                            ILocalThread ilt = vmm.getThreadManager().getLocalThread(lt_uuid_wrap);
                            VirtualStackframe vsf = new VirtualStackframe(null, fullOp, ilt, parent);
                            vsf.setCallBase(new Integer(base));
                            if(ilt == null) vsf.flagAsDamaged("Thread being pushed doesn't exist.");

                            current.lock();
                            assert (current.virtualStack().peek().getOutboundOperation()
                                    .equals(op));
                            /* Updates the local-to-distributed thread index */
                            threads2dthreads.put(lt_uuid_wrap, dt_uuid_wrap);

                            VirtualStack vs = current.virtualStack();
                            vs.pushFrame(vsf);
                            
                            if(stackLogger.isDebugEnabled()){
                                stackLogger.debug("Pushed frame: "
                                        + "\n Full operation: " + fullOp 
                                        + "\n Distributed thread: " + fmh.uuid2Dotted((int)dt_uuid)
                                        + "\n Server-side thread: " + fmh.uuid2Dotted((int)lt_uuid));
                            }
                            
                            if(vs.length() >= MAXIMUM_STACK_SIZE){
                                if(!current.isSuspended())
                                    current.suspend();
                                logger.error("Stack overflow. Distributed thread " + fmh.uuid2Dotted((int)dt_uuid) + " is suspended. ");
                                throw new DistributedStackOverflowException();
                            }
                            
                        }
                    } finally {
                        // Releases the lock before making remote communications.
                        unlockAllTables();
                    }

                    mode = current.getMode();
                    
                    if(stackBuilderMode) break;
                    
                    /*
                     * Makes the request if and only if the thread is remote
                     * STEPPING_REMOTE and STEPPING_INTO. It matters here if 
                     * the thread is STEPPING_INTO because if it is then we
                     * must stop it a the server side. If the user stepped 
                     * over the remote call, however, it doesn't matter that
                     * the thread is in stepping mode because we should go 
                     * over the call anyway (unless the user inserts a breakpoint
                     * at the server side).
                     */
                    if (/*(mode & DistributedThread.ILLUSION)  != 0 &&*/ (mode & DistributedThread.STEPPING) != 0) {
                        /**
                         * Old code was:
                         *                                                 
                         * DeferrableBreakpointRequest bp = new DeferrableBreakpointRequest(
                         *       vmm.getName(), fullOp, null);
                         *       
                         * But we're migrating to absolute line breakpoints until we can
                         * process annotations. That's because just having the method name
                         * was causing some AmbiguousSymbolExceptions to be thrown.
                         */       
                        IBreakpoint bkp = vmm.setBreakpointFromEvent(evt);
                        current.setServersideBreakpoint(bkp);
                    }

            	} finally {
                    if (current != null){
                        current.unlock();
                    }
                }
            	            	
                break;
            }
            
            case DebuggerConstants.SIGNAL_BOUNDARY:
            {
                String dtuid = evt.getAttribute("dtid");
                String ltuid = evt.getAttribute("ltid");
                String fullOp = evt.getAttribute("fop");
                String base = evt.getAttribute("siz");

                int dt_uuid = fmh.hex2Int(dtuid);
                int lt_uuid = fmh.hex2Int(ltuid);

                Integer dt_uuid_wrap = new Integer(dt_uuid);
                Integer lt_uuid_wrap = new Integer(lt_uuid);

                DistributedThread current = null;
                DistributedThread.VirtualStack vs;
                VirtualStackframe vf;

                try {
                    try {
                        /*
                         * Be disciplined with lock acquisition. First the
                         * tables, then the thread lock.
                         */
                        lockAllTables();
                        current = (DistributedThread) dthreads
                                .get(dt_uuid_wrap);
                        assert (current != null);
                        current.lock();

                        vs = current.virtualStack();
                        /*
                         * The assertion is that we must have a caller client,
                         * or this message shouldn't have been sent in the first
                         * place.
                         */
                        assert (vs.getVirtualFrameCount() >= 2);

                        VirtualStackframe vsf = vs.peek();
                        assert (vsf.getLocalThreadId().intValue() == lt_uuid);

                        vf = vs.popFrame();

                        /* Updates the local-to-distributed thread index */
                        assert (threads2dthreads.containsKey(lt_uuid_wrap));
                        threads2dthreads.remove(lt_uuid_wrap);

                    } finally {
                        /* Releases table locks to reduce contention. */
                        unlockAllTables();
                    }
                    
                    /* Asserts that the operation we're returning from 
                     * is the same operation as the one that went in.
                     */
                    assert vf.getInboundOperation().equals(fullOp);
                    assert vf.getCallBase() == Integer.parseInt(base);

                    /* Resumes the current thread if it's stepping. 
                     * 
                     * We check if the thread is stepping by examining
                     * if there are pending step requests for it.
                     *  
                     * */
                    if(vf.getThreadReference().hasPendingStepRequests())
                        mode = (byte)DistributedThread.STEPPING;
                    else mode = DistributedThread.RUNNING;
                    
                    if(stackLogger.isDebugEnabled()){
                        stackLogger.debug("Server return processed: "
                                + "\n Operation name: " + fullOp 
                                + "\n Distributed thread: " + fmh.uuid2Dotted((int)dt_uuid)
                                + "\n Returning thread: " + fmh.uuid2Dotted((int)lt_uuid));
                    }
                        
                    
                    if(stackBuilderMode) break;
                    
                    if (/*(mode & DistributedThread.ILLUSION) != 0 &&*/
                    		(mode & DistributedThread.STEPPING) != 0) {
                        ILocalThread tr = vmm.getThreadManager().getLocalThread(lt_uuid);

                        // assert(tr.isSuspended()); // buggy assumption
                        /*
                         * It's buggy because the thread isn't actually
                         * suspended, it's blocked at object.wait and hasn't
                         * fulfilled the next step request yet. I'll leave it
                         * here so I don't feel tempted to insert this assert
                         * for the third time.
                         * 
                         * OK. We have to clear the step requests. Remeber this
                         * local thread is stepping.
                         */
                        tr.clearPendingStepRequests();

                        /*
                         * We must arrange for the caller client thread to stop
                         * when the distributed thread gets back there.
                         *
                         * setClientReturnBreakpoint(vf); - not done this way
                         * anymore
                         *
                         * I don't know if this thread will ever be in suspended
                         * state, but we're better safe than sorry (I have to do
                         * some thinking before removing this.)
                         * 
                         */
                        if (tr.isSuspended()) {
                            tr.resume();
                        }
                    }
                    
                } finally {
                    if (current != null)
                        current.unlock();
                }

                break;
            }

            default:
                ret.setStatus(DebuggerConstants.PROTOCOL_ERR);
                ret.setStatus(DebuggerConstants.UNKNOWN_EVENT_TYPE_ERR);
                mode = DistributedThread.UNKNOWN;
                break;
            }
            
        } catch (ParserException e) {
            ret = new ByteMessage(1);
            ret.setStatus(DebuggerConstants.PROTOCOL_ERR);
            ret.writeAt(0, DebuggerConstants.HANDLER_FAILURE_ERR);
            mode = DistributedThread.UNKNOWN;
            logger.error("Error while parsing message - format error.",e);
        } catch (IllegalAttributeException e){
            ret = new ByteMessage(1);
            ret.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            ret.setStatus(DebuggerConstants.ICW_ILLEGAL_ATTRIBUTE);
            mode = DistributedThread.UNKNOWN;
            logger.error("Required paremeter missing - wrong message content.",e);
        } catch(Exception e) {
            ret = new ByteMessage(0);
            ret.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            mode = DistributedThread.UNKNOWN;
            logger.error("Caught exception", e);
        }
        
        if(ret == null){
            /*
             * We now share our knowledge with the local agent.
             */
            ret = new ByteMessage(1);
            ret.setStatus(DebuggerConstants.OK);
            /*
             * How lame. I use EVENT_TYPE_IDX as if I didn't knew it
             * is zero, but in fact, if it's different from zero we
             * get an array out of bounds exception.
             */
            assert mode != 0;
            ret.writeAt(DebuggerConstants.EVENT_TYPE_IDX, mode);
        }

        return ret;
    }
    
    protected void notifyDeath(Integer dt_uuid)
    {
        try{
            this.lockAllTables();
            assert(dthreads.containsKey(dt_uuid));
            assert(threads2dthreads.contains(dt_uuid));
            DistributedThread dt = dthreads.get(dt_uuid);
            assert(dt.virtualStack().getVirtualFrameCount() == 1);
            dthreads.remove(dt_uuid);
            threads2dthreads.remove(dt_uuid);
        }finally{
            this.unlockAllTables();
        }
        
    }
    
    /**
     * This method should be called whenever locks to both tables
     * must be acquired. It imposes an ordering on lock acquisition
     * which prevents deadlocks from occurring.
     *
     */
    private void lockAllTables(){
        dthreads.lockForWriting();
        threads2dthreads.lockForWriting();
    }
    
    /**
     * This method should be called to release locks acquired by
     * lockAllTables.
     *
     */
    private void unlockAllTables(){
        threads2dthreads.unlockForWriting();
        dthreads.unlockForWriting();
    }
    
    /**
     * Locks all internal tables for reading. Locks are acquired
     * in the same order as in lockAllTables.
     * <b>Please</b> be careful. This method will lock the 
     * thread tracking mechanism. This means you should <b>NOT</b>
     * forget to call endSnapshot unless you'd like to see some
     * buffer overflows.
     */
    public synchronized void beginSnapshot(){
        threads2dthreads.lockForReading();
        dthreads.lockForReading();
    }
    /**
     * Unlocks all internal tables for reading. Locks are relinquished
     * in the same order as in unlockAllTables.
     */ 
    public synchronized void endSnapshot(){
        dthreads.unlockForReading();
        threads2dthreads.unlockForReading();
    }

    public IThread[] getThreads() {
        try{
            beginSnapshot();
            Collection <DistributedThread> dThreadsCol = dthreads.values();
            DistributedThread [] dThreads = new DistributedThread[dThreadsCol.size()];
            return dthreads.values().toArray(dThreads); 
        }finally{
            endSnapshot();
        }
    }

    public boolean hasThreads() {
        try{
            dthreads.lockForReading();
            return !dthreads.isEmpty();
        }finally{
            dthreads.unlockForReading();
        }
        
    }

    // Maybe I'll remove these ops.
    public void resumeAll() throws Exception { }

    public Integer getThreadUUID(IThread tr) { 
        return null;
    }

    public Object getAdapter(Class adapter) {
        if(adapter.isAssignableFrom(this.getClass())) return this;
        return null;
    }
}
