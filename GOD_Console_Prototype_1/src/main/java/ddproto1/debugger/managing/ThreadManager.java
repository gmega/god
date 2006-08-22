/*
 * Created on Aug 13, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ThreadManager.java
 */

package ddproto1.debugger.managing;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IThread;

import com.sun.jdi.ClassType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.TaggerProxy;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.util.MessageHandler;
import ddproto1.util.MethodSearchUtil;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * @author giuliano
 *
 */
public class ThreadManager implements
        IJavaThreadManager, IJDIEventProcessor {
    
    public static final String module = "ThreadManager -";
    private static final ConversionUtil fh = ConversionUtil.getInstance();
    private static final MessageHandler mh = MessageHandler.getInstance();
    private static final ProcessingContextManager pcm = ProcessingContextManager.getInstance();
    
    private ThreadReference currentThread = null;
    private IDebugContext dc;
   
    private boolean enabled = true;
    private IJDIEventProcessor next;
    
    /* Thanks to JDI we have to use this ugly, explicit mapping (I had
     * a much neatier solution). */
    private Map <Integer, ThreadReference> uuid2thread;
    private Map <ThreadReference, Integer> thread2uuid;
    
    /** Language independent thread references. */
    private BidiMap liThreads;

    public ThreadManager(IDebugContext dc){
        this.dc = dc;
        this.uuid2thread = new HashMap <Integer, ThreadReference> ();
        this.thread2uuid = new HashMap <ThreadReference, Integer> ();
        this.liThreads   = new DualHashBidiMap();
    }
    
    public void specializedProcess(Event e) {
        try{
            IProcessingContext pc = pcm.getProcessingContext();
            if(e instanceof VMStartEvent){
                handleVMStart((VMStartEvent)e);
            }else if(e instanceof ThreadStartEvent){
                registerThread2Id(ThreadManager.getThreadFromEvent(e));
                pc.vote(IEventManager.RESUME_SET);
            }
            else if(e instanceof ThreadDeathEvent){
                unregisterThread(ThreadManager.getThreadFromEvent(e));
                pc.vote(IEventManager.RESUME_SET);
            }
            else if(e instanceof BreakpointEvent){
                /* We only resume threads stopped by our registration breakpoints. 
                 * Remove this if clause and breakpoints will no longer work.     */
                if(registerId2Thread(e)){
                    pc.vote(IEventManager.RESUME_SET);
                }
            }else if(e instanceof VMDeathEvent || e instanceof VMDisconnectEvent){
            	handleVMDeath(e);
            }else
                throw new UnsupportedException(module + "Received an unsupported event.");
            
        }catch(InvocationException ex){
            mh.getStandardOutput().println(
                    "This exception was originated at remote VM "
                            + dc.getVMM().getName()
                            + ", it's only being shown here.");
            mh.printStackTrace(ex.getCause());
        }catch(Exception ex){
            mh.printStackTrace(ex);
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMThreadManager#suspendAll()
     */
    public synchronized void suspendAll() throws Exception {
        dc.getVMM().virtualMachine().suspend();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMThreadManager#resumeAll()
     */
    public synchronized void resumeAll() throws Exception {
        dc.getVMM().virtualMachine().resume();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMThreadManager#getThreads()
     */
    public synchronized List <Integer> getThreadIDList() {
        /* Returns a copy in order to avoid ConcurrentAccessExceptions. The
         * list might become outdated the second its created, however.
         */ 
        return new ArrayList<Integer>(uuid2thread.keySet());
    }
    
    public synchronized ILocalThread[] getLocalThreads(){
        return this.liThreads();
    }
    
    public synchronized IThread[] getThreads(){
        return this.liThreads();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMThreadManager#setCurrentThread(com.sun.jdi.ThreadReference)
     */
    public void setCurrentThread(ThreadReference tr) {
        currentThread = tr;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMThreadManager#getCurrentThread()
     */
    public ThreadReference getCurrentThread() 
    {
        /* TODO Must check if this info might not be outdated because
         * of race conditions (don't know if this could actually happen).
         */ 
        return currentThread;
    }
    
    public synchronized boolean registerThread2Id(ThreadReference tr){
        // Guard against duplicates.
        if (thread2uuid.containsKey(tr)) {
            return false;
        } else {
            Integer id = null;
            
            /* If the threadreference has already been registered,
             * we must determine its id.
             */
            if(uuid2thread.containsValue(tr)){
                Iterator it = uuid2thread.keySet().iterator();
                while(it.hasNext()){
                    Integer tmp = (Integer)it.next();
                    ThreadReference curr = (ThreadReference)uuid2thread.get(tmp);
                    if(curr.equals(tr)){
                        id = tmp;
                        break;
                    }
                }
            }
            
            thread2uuid.put(tr, id);
            
            /** Add a wrapper. */
            this.addThread(tr);
            
            return true;
        }
    }
    
    public synchronized boolean unregisterThread(ThreadReference tr){
        assert(thread2uuid.containsKey(tr));
        Integer uuid = (Integer)thread2uuid.remove(tr);
        
        if(uuid != null)
            assert(uuid2thread.containsKey(uuid));
        
        if(uuid2thread.containsKey(uuid))
            uuid2thread.remove(uuid);
        
        IThread eclipseThread = this.getAdapterForThread(tr);
        assert eclipseThread != null;
        
        if(eclipseThread instanceof DebugElement){
            ((DebugElement)eclipseThread).fireTerminateEvent();
        }else{
            mh.getErrorOutput().println("Don't know how to notify termination of" +
                    " classes that don't extend DebugElement.");
        }
        
        return true;
    }
    
    public boolean registerId2Thread(Event e) throws Exception {
        BreakpointEvent bpe = (BreakpointEvent) e;

        ThreadReference current = bpe.thread();

        String called = fh.simpleMethodName(bpe.location().method().name());
        String desired = fh
                .simpleMethodName(DebuggerConstants.TAGGER_REG_METHOD_NAME);

        /* This isn't a "registration breakpoint". Just return. */
        if (!called.equals(desired))
            return false;

        /*
         * We might be frozen here for a while. Launch another thread to handle
         * the other events in case we freeze (otherwise the program stops).
         * 
         * If I could ensure that at this point the suspend count will always be
         * 1, this problem would go away.
         */
        dc.getVMM().getDispatcher().handleNext();
        Integer id = extractId(current);

//        if (current.isSuspended()) {
//            int delta = current.suspendCount();
//            for(int i = 0; i < delta; i++)
//                current.resume();
//        }

        /*
         * This method used to be entirely synchronized but we were forced to
         * narrow it down due to a deadlock. It all came for the best, though,
         * since we only need synchronization while playing around with the
         * thread Maps anyway.
         */
        synchronized (this) {
            assert (!(uuid2thread.containsKey(id)));
            uuid2thread.put(id, current);
            thread2uuid.put(current, id);
            getAdapterForThread(current).setGUID(id);
        }

        /*
         * It's wrong to think the suspend count should be zero here
         * since we're resuming the thread and it might as well just 
         * hit another suspending event.
         */
        //assert (current.suspendCount() == 0);
        return true;
    }
    
    private Integer extractId(ThreadReference tr)
    	throws Exception
    {
        VirtualMachine vm = dc.getVMM().virtualMachine();
        TaggerProxy tagger = TaggerProxy.getInstance();
        ClassType taggerClass = tagger.getClass(vm, DebuggerConstants.TAGGER_CLASS_NAME);
        
        IntegerValue uuid = tagger.getRemoteUUID(tr, taggerClass);
        
        return new Integer(uuid.intValue());
    }
    
    /** Checks target VM thread-by-thread and sees if any there are
     * any threads running. You shouldn't call this operation too often
     * since iterating through all threads in a JVM is a bit expensive 
     * (for a loop condition, for example).
     * 
     * @return
     */
    public synchronized boolean allThreadsSuspended(){
        Iterator it = dc.getVMM().virtualMachine().allThreads().iterator();
        
        boolean suspended = true;
        while(it.hasNext()){
            suspended &= ((ThreadReference)it.next()).isSuspended();
        }
        
        return suspended;
    }
    
    public synchronized ThreadReference findThreadById(long uid){
        ThreadReference target = null;
        Iterator it = dc.getVMM().virtualMachine().allThreads().iterator();
        while(it.hasNext()){
            target = (ThreadReference)it.next();
            if(target.uniqueID() == uid) break;
            else target = null;
        }
        return target;
    }
    
    public boolean hasThreads(){
        synchronized(this){
            return !thread2uuid.isEmpty();
        }
    }
    
    public JavaThread getAdapterForThread(ThreadReference tr){
        synchronized(liThreads){
            return (JavaThread)liThreads.get(tr);
        }
    }
    
    public synchronized JavaThread getLIThread(Integer id){
        return getAdapterForThread(findThreadByUUID(id));
    }
    
    public synchronized ThreadReference findThreadByUUID(int uuid){
        return findThreadByUUID(new Integer(uuid));
    }
    
    public synchronized ThreadReference findThreadByUUID(Integer uuid){
        return (ThreadReference)uuid2thread.get(uuid);
    }
    
    public synchronized Integer getThreadUUID(ThreadReference tr){
        return (Integer)thread2uuid.get(tr);
    }
    
    public static ThreadReference getThreadFromEvent(Event e) throws Exception {
        MessageHandler mh = MessageHandler.getInstance();
        Class evClass = e.getClass();
        ThreadReference tr = null;
        try {
            Method getThread = MethodSearchUtil.getInstance().searchHierarchy(
                    evClass, "thread", null, 5);
            if (getThread == null)
                throw new NoSuchMethodException(module
                        + " Cannot obtain thread from event " + e);
            getThread.setAccessible(true);
            tr = (ThreadReference) getThread.invoke(e, (Object []) null);
        } catch (Exception ex) {
            mh.getWarningOutput().println(
                    module + " Error while obtaining current "
                            + "thread. Not very sure on what's the "
                            + "meaning of this.");
            throw ex;
        }
        return tr;
    }
    
    protected void addThread(ThreadReference tr){
        JavaThread jt;
        
        synchronized(liThreads){
            if(liThreads.containsKey(tr)) return;
            IJavaDebugTarget ijdt = (IJavaDebugTarget)dc.getVMM().getAdapter(IJavaDebugTarget.class);
            if(ijdt == null) return;
            jt = new JavaThread(tr, ijdt);
            liThreads.put(tr, jt);
        }
        
        jt.fireCreationEvent();
    }
    
    protected void releaseThread(JavaThread jt){
        synchronized(liThreads){
            liThreads.remove(jt.getJDIThread());
        }
        
        jt.fireTerminateEvent();
    }
    
    protected ILocalThread [] liThreads(){
        synchronized(liThreads){
            ILocalThread [] tArray = new ILocalThread[liThreads.size()];
            return (ILocalThread [])liThreads.values().toArray(tArray);
        }
    }
   
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.DeferrableEventRequest#getAssociatedVM()
     */
    protected VirtualMachine getAssociatedVM() throws VMDisconnectedException {
        return dc.getVMM().virtualMachine();
    }
    
    protected void handleVMStart(VMStartEvent ex){
        /** Attempts to grab all running threads. */
        for(ThreadReference tr : (List<ThreadReference>)ex.virtualMachine().allThreads()){
            registerThread2Id(tr);
        }
    }
    
    protected void handleVMDeath(Event e){
    	synchronized(liThreads){
    		Iterator<JavaThread> it = liThreads.values().iterator();
    		while(it.hasNext()){
    			JavaThread jt = it.next();
    			jt.fireTerminateEvent();
    			it.remove();
    		}
    	}
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#process(com.sun.jdi.event.Event)
     */
    public void process(Event e) 
        throws Exception
    {
        if(enabled) specializedProcess(e);
        if(next != null) next.process(e);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#setNext(ddproto1.debugger.eventhandler.processors.IJDIEventProcessor)
     */
    public void setNext(IJDIEventProcessor iep) {
        next = iep;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#getNext()
     */
    public IJDIEventProcessor getNext() {
        return next;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#enable(boolean)
     */
    public void enable(boolean status) {
        enabled = status;
    }

    public void notifyVMSuspend() { }
    
    public void notifyVMResume() 
    	throws DebugException
    {
        synchronized(liThreads){
        	for(JavaThread jt : (Iterable <JavaThread>)liThreads.values())
        		jt.resumedByVM();
        }
    }

    public Integer getThreadUUID(IThread tr) {
        /**
         * I suck with concurrency. I'm acquiring these two locks because I think 
         * I need a read barrier with thread2uuid, but I might as well be wrong. 
         */
        synchronized(liThreads){
            synchronized(this){
                return thread2uuid.get((ThreadReference)liThreads.inverseBidiMap().get(tr));
            }
        }
    }

    public ILocalThread getLocalThread(Integer uuid) {
        return getLIThread(uuid);
    }
    
    public IThread getThread(Integer uuid){
        return getLIThread(uuid);
    }

    public Object getAdapter(Class adapter) {
        if(adapter.isAssignableFrom(this.getClass())) return this;
        return null;
    }
}