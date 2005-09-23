/*
 * Created on Sep 23, 2004
 *
 */
package ddproto1.debugger.managing.tracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;


import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;

import ddproto1.util.commons.ByteMessage;
import ddproto1.util.commons.Event;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread.VirtualStack;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.DeferrableHookRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdResolutionContextImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.debugger.server.IRequestHandler;
import ddproto1.exception.DistributedStackOverflowException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoSuchElementError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.PropertyViolation;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.ParserException;
import ddproto1.interfaces.IUICallback;
import ddproto1.util.collection.LockingHashMap;
import ddproto1.util.traits.JDIMiscTrait;
import ddproto1.util.traits.commons.ConversionTrait;

/**
 * The <b>Distributed Thread Manager</b> is a critical component, 
 * responsible for providing access to system-wide distributed threads 
 * as well as for collecting update information from out-of-band 
 * and in-band event dispatchers and merging them into an approximation
 * of the distributed systems state (toghether with other components).
 * 
 * 
 * @author giuliano
 *
 */
public class DistributedThreadManager implements IRequestHandler {
    
    private static final int MAXIMUM_STACK_SIZE = 99;
    private static final Logger logger = Logger.getLogger(DistributedThreadManager.class);
    private static final Logger stackLogger = Logger.getLogger(DistributedThreadManager.class.getName() + ".stackLogger");
    
    private static ConversionTrait fmh = ConversionTrait.getInstance();
    private static JDIMiscTrait jmt = JDIMiscTrait.getInstance();
    private static IHeadCounter theCounter = new JDIHeadCounter();
    
    /** This flag is for debugging purposes.
     * When in stack builder mode, the tracker simply builds the 
     * distributed stack without concerning itself with breakpoints 
     * or any VMM related stuff. In fact, it avoids acessing the
     * VMMs altogether. This allows us to run both the debugger
     * and the debuggees under another debugger and follow their flow.
     */
    private static final boolean stackBuilderMode = false;

    private DTStateUpdater dtsu;
    
    private Map <Byte, Node> nodes;
    private LockingHashMap <Integer, DistributedThread> dthreads;
    private LockingHashMap <Integer, Integer> threads2dthreads;
    
    private IUICallback ui;

    public DistributedThreadManager(IUICallback ui){ 
        nodes = new HashMap<Byte, Node>();
        dthreads = new LockingHashMap<Integer, DistributedThread>(true);
        threads2dthreads = new LockingHashMap<Integer, Integer>(true);
        this.ui = ui;
        dtsu = new DTStateUpdater(this);
    }
    
    public void registerNode(VirtualMachineManager vmm)
    {
        /* Node registration is a complicated, error-prone process. The main 
         * cause for issues here concerns the hook requests, which make no guarantees
         * as to where the actual hooks will be placed in the processing chain.
         * REMARK A less loose hook request would be nice, though it will probably require
         * a radical redesign of our event dispatcher (I´ll be thinking about it though). 
         */
        Byte gid = new Byte(vmm.getGID());
        
        Node spec = new Node();
        
        Set <Integer> policySet = new HashSet<Integer>();
        policySet.add(new Integer(EventRequest.SUSPEND_ALL));
        policySet.add(new Integer(EventRequest.SUSPEND_EVENT_THREAD));
        
        try{
            /* Obtains the stublist (or gets an exception if this node
             * doesn't define it).
             */
            String stublist = vmm.getAttribute("stublist");
            String skellist = vmm.getAttribute("skeletonlist");
            Set <String> stubclasses = new HashSet<String> ();
            Set <String> skelclasses = new HashSet<String> ();
            StringTokenizer st = new StringTokenizer(stublist, IConfigurationConstants.LIST_SEPARATOR_CHAR);
            while(st.hasMoreTokens())
                stubclasses.add(st.nextToken());
        
            st = new StringTokenizer(skellist, IConfigurationConstants.LIST_SEPARATOR_CHAR);
            while(st.hasMoreTokens())
                skelclasses.add(st.nextToken());
            
            ComponentBoundaryRecognizer cbr = new ComponentBoundaryRecognizer(this, stubclasses, ui, vmm);
                        
            spec.gid = gid;
            spec.vmm = vmm;
            
            /* Now adds the hook requests, which we hope will go into the right place */
            spec.cbr = new DeferrableHookRequest(vmm.getName(), IEventManager.STEP_EVENT, cbr, policySet);
            spec.dtsu_td = new DeferrableHookRequest(vmm.getName(), IEventManager.THREAD_DEATH_EVENT, dtsu, null);
            spec.dtsu_step = new DeferrableHookRequest(vmm.getName(), IEventManager.STEP_EVENT, dtsu, null);
            vmm.getDeferrableRequestQueue().addEagerlyResolve(spec.cbr);
            vmm.getDeferrableRequestQueue().addEagerlyResolve(spec.dtsu_td);
            vmm.getDeferrableRequestQueue().addEagerlyResolve(spec.dtsu_step);
                        
        }catch (IllegalAttributeException e){
            /* Not an error - just means this node is not middleware-enabled */
        }catch(Exception e){
            logger.error("Failed to register node " + vmm.getName() + " in the distributed thread manager.");
            return;
        }
        
        nodes.put(spec.gid, spec);
    }
    
    public void unregisterNode(byte gid)
        throws NoSuchSymbolException, InternalError
    {
        /* The registration process is so complicated that we would go nuts if we would like
         * to allow nodes to be unregistered.
         * 
         * CORRECTION: Might not be that bad. If you look closely you'll realise that the only thing
         * that actually gets registered is the Node structure. Also, after I added support for 
         * deferrable request removal into the DeferrableRequestQueue class, things got a little better.
         */
        
        Node spec = nodes.get(gid);
        if(spec == null) throw new NoSuchSymbolException("Node " + gid + " doesn't exist.");
        if(spec.cbr != null){
            try{
                spec.cbr.undo();
                assert spec.dtsu_step != null;
                spec.dtsu_step.undo();
                assert spec.dtsu_td != null;
                spec.dtsu_td.undo();
            }catch(Exception ex){
                throw new InternalError("Unregistration of node " + gid + " could not be completed.", ex);
            }
        }
    }
    
    public DistributedThread getByUUID(int uuid) 
    	throws NoSuchElementError {
        return getByUUID(new Integer(uuid));
    }
    
    public DistributedThread getByUUID(Integer uuid)
    	throws NoSuchElementError
    {
        try{
            dthreads.lockForReading();
            DistributedThread dt = dthreads.get(uuid);
            if(dt == null)
                throw new NoSuchElementError("Required distributed thread doesn't exist.");
            return dt;
        }finally{
            dthreads.unlockForReading();
        }
    }
    
    public synchronized Iterator <Integer> getThreadIDList(){
        try{
            dthreads.lockForReading();
            return dthreads.keySet().iterator();
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
        ByteMessage ret = null;
        byte mode;
        
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(gid);
        if(vmm == null){
            logger.error("Invalid gid " + gid + ". Assertion failed.");
            throw new AssertionError("vmm == null");
        }
            
                
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
             *        arise because of it and I'm getting convinced we could stuff this all at the server
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
            	try {
                    /*
                     * Only starts playing around with objects if we can
                     * provide atomic modifications to table readers.
                     */
            	    lockAllTables();
            	    
            	    VirtualStackframe vsf;

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
                        vsf = new VirtualStackframe(op, null,
                                lt_uuid_wrap);

                        /** Thread inherits the state of the local thread. 
                         * Since the thread got this far, it's possible states are:
                         * 1) RUNNING
                         * 2) STEPPING_INTO
                         * 3) STEPPING_OVER 
                         */
                        Byte inmode = vmm.getLastStepRequest(lt_uuid);
                        if(inmode == null) mode = DistributedThread.RUNNING;
                        else mode = (byte)(inmode | DistributedThread.STEPPING_REMOTE);
                        
                        if(!stackBuilderMode){
                            current = new DistributedThread(vsf, vmm
                                    .getThreadManager(), mode, theCounter);
                        }else{
                            current = new DistributedThread(vsf, null, mode, null);
                        }
                        
                        dthreads.put(dt_uuid_wrap, current);

                        /* Updates the local-to-distributed thread index */
                        threads2dthreads.put(lt_uuid_wrap, dt_uuid_wrap);
                        
                        vsf.setCallTop(st_size_wrap);

                        unlockAllTables();
                        
                        if(stackBuilderMode) break;

                        /**
                         * We remove all client-side step requests. It's the
                         * client-side interceptor who should decide whether
                         * or not to stop the thread when it returns.
                         */
                        if ((mode & DistributedThread.STEPPING_INTO) != 0
                                || (mode & DistributedThread.STEPPING_OVER) != 0) {
                            ThreadReference tr = vmm.getThreadManager()
                                    .findThreadByUUID(lt_uuid);
                            jmt.clearPreviousStepRequests(tr, vmm);
                        }
                        
                        /*
                         * We've met a prencondition. Inform whomever might be
                         * interested.
                         */
                        StdPreconditionImpl spi = new StdPreconditionImpl();
                        spi.setType(new StdTypeImpl(IDeferrableRequest.THREAD_PROMOTION, IDeferrableRequest.MATCH_ONCE));
                        spi.setClassId(ltuid);

                        StdResolutionContextImpl srci = new StdResolutionContextImpl();
                        srci.setContext(this);
                        srci.setPrecondition(spi);
                        
                        vmm.getDeferrableRequestQueue().resolveForContext(srci);
                        
                        /** We remove all client-side step requests. It's the client-side interceptor 
                         * who should decide whether or not to stop the thread when it returns.
                         */
                        ThreadReference tr = vmm.getThreadManager().findThreadByUUID(lt_uuid);
                        jmt.clearPreviousStepRequests(tr, vmm);

                        
                        /** Get the freshest mode to return to the client (updated after
                         * the precondition broadcast).
                         */                       
                        mode = current.getMode();
                        
                    }
                    
                    else{
                    	current = (DistributedThread) dthreads
								.get(dt_uuid_wrap);
                    	vsf = current.virtualStack().peek();
                        assert(vsf != null);
                    	current.lock();
                        vsf.setCallTop(st_size_wrap);
                        vsf.setOutboundOperation(op);
                        
                        mode = current.getMode(); 
                        
                        current.unlock();
                        unlockAllTables();
                    }
                
            	} catch(RuntimeException e) {
                    unlockAllTables();
                    if(current != null)
                        current.unlock();
                    throw e;
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
                String brLine = evt.getAttribute("lin");
                String clsName = evt.getAttribute("cls");
                
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
                            VirtualStackframe vsf = new VirtualStackframe(null, fullOp,
                                    lt_uuid_wrap);
                            vsf.setCallBase(new Integer(base));

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
                                        + "\n Server-side thread: " + fmh.uuid2Dotted((int)dt_uuid));
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
                    if ((mode & DistributedThread.STEPPING_REMOTE) != 0 && (mode & DistributedThread.STEPPING_INTO) != 0) {
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
                        DeferrableBreakpointRequest bp = new DeferrableBreakpointRequest(
                                vmm.getName(), clsName, Integer.parseInt(brLine));

                        /** This is really great. 
                         * The two lines of code that follow ensure that:
                         * 
                         * 1) This breakpoint will only halt the correct thread.
                         * 2) This breakpoint will remove itself after serving its purpose,
                         *    without affecting other threads or other user breakpoints. 
                         */
                        bp.addThreadFilter(lt_uuid_wrap);
                        bp.setOneShot(true);
                        
                        vmm.getDeferrableRequestQueue().addEagerlyResolve(bp);
                    }

            	} finally {
                    if (current != null)
                        current.unlock();
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
                     * The other possible states are:
                     * 1) RUNNING - No need to resume the thread.
                     * 2) SUSPENDED - Then it shouldn't be sending messages. 
                     * 
                     * It doesn't matter if the thread is STEPPING_INTO or 
                     * STEPPING_OVER, it must be resumed if it's suspended.
                     *  
                     * */
                    mode = current.getMode();
                    
                    if(stackBuilderMode) break;
                    
                    if ((mode & DistributedThread.STEPPING_REMOTE) != 0) {
                        ThreadReference tr = vmm.getThreadManager()
                                .findThreadByUUID(lt_uuid);

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
                        jmt.clearPreviousStepRequests(tr, vmm);

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
            logger.error(e);
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
    

    private class Node{
        protected VirtualMachineManager vmm;
        protected Byte gid;
        protected DeferrableHookRequest cbr;
        protected DeferrableHookRequest dtsu_td;
        protected DeferrableHookRequest dtsu_step;
    }
}
