/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: EventHandlerImpl.java
 */

package ddproto1.debugger.managing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.model.IDebugTarget;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequest;

import ddproto1.debugger.eventhandler.IEventHandler;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.IVotingManager;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.exception.IllegalStateException;
import ddproto1.exception.NoContextException;
import ddproto1.exception.TargetRequestFailedException;
import ddproto1.interfaces.ISemaphore;
import ddproto1.util.ExternalSemaphore;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class EventDispatcher implements IVotingManager {
    
    private static final int MAX_SIMULTANEOUS_EVENTS = 8;
    
    private static final ProcessingContextManager pcm = ProcessingContextManager.getInstance();
    private static final Logger logger = MessageHandler.getInstance().getLogger(EventDispatcher.class);
    private static final IJavaNodeManagerRegistry vmmf = VMManagerFactory.getRegistryManagerInstance();

    private VirtualMachine jvm;
    private EventQueue queue;
    private String jvmid;
    private IEventHandler handler;
    private boolean connected = false;
    private boolean removing = false;
    private boolean takesNew = true;
    
    private IDebugContext dc;
    
    private ISemaphore sema = new ExternalSemaphore(1, this);
    
    private Set <String >voteSet = new HashSet<String>();
    
    /* Though we keep only one event set stored in state variables at any
     * given point in time, we might have multiple event sets being operated
     * by delayed threads.
     */
    private EventSet events;
    private List<JavaThread> enlistedThreads 
        = new LinkedList<JavaThread>(); 
    
    private Iterator it;
    private Thread owner;
    private int handlerThreads = 0;
    private Set <Thread> markToDie = new HashSet <Thread>();
    
    /* This keeps event counter associated with each eventSet.
     */
    private Map <EventSet, Integer> event2int = new HashMap<EventSet, Integer>();
    
    public EventDispatcher(IEventHandler handler) {
        this.handler = handler;
        voteSet.add(IEventManager.RESUME_SET);
    }
    
    private void doRun() {
        try {
            if (dc == null)
                throw new IllegalStateException(
                        "Error - debug context not configured.");

            if (!connected) {
                connected = true;
                this.jvm = dc.getVMM().virtualMachine();
                this.jvmid = dc.getVMM().getName();
                this.queue = jvm.eventQueue();
            }

            live();

            EventSet tlocalEvents = null;
            /* Event dispatching loop */
            while (connected) {

                /*
                 * We can't afford to have two threads waiting for events - it's
                 * an error.
                 */
                sema.p();
                if (removing == true) {
                    /* Current thread dies with an error. */
                    MessageHandler mh = MessageHandler.getInstance();
                    mh.getErrorOutput().println(
                            "Error - you cannot handle another event while no event"
                                    + " set has yet been grabbed.");
                    die();
                    sema.v();
                    return;
                }

                /* No more events to process on this set. Grab another. */
                if (it == null || !it.hasNext()) {
                    removing = true;
                    sema.v();
                    events = queue.remove();
                    event2int.put(events, new Integer(events.size()));
                    it = events.iterator();
                } else {
                    sema.v();
                }

                /*
                 * Retrieves the processing context for this processing thread .
                 */
                ProcessingContextImpl pci = (ProcessingContextImpl) pcm
                        .getProcessingContext();
                ((ProcessingContextImpl) pcm.getProcessingContext()).reset();

                /* Now new threads can be created. */
                removing = false;

                while (true) {
                    Event evt;

                    /*
                     * Lock contention shouldn't be an issue since under normal
                     * circumstances we'll hardly have more than one thread
                     * running.
                     * 
                     * Synchronization is to guarantee that each thread that
                     * does not break actually gets an event to process that is
                     * distinct from the event being handled by other threads.
                     */
                    synchronized (this) {
                        if (!it.hasNext())
                            break;
                        evt = (Event) it.next();
                        /*
                         * This thread is now officially operating on the
                         * current event set.
                         */
                        tlocalEvents = events;
                    }

                    pci.setProcessing(true);
                    processEvent(evt);

                    /*
                     * Updates the events to go. Note that we use tlocalEvents
                     * since the current event set might have already been
                     * changed.
                     */
                    synchronized (this) {
                        Integer counter = event2int.get(tlocalEvents);
                        event2int.put(tlocalEvents, new Integer(counter
                                .intValue() - 1));
                    }

                    pci.setProcessing(false);

                    /*
                     * No need to handle the other events - another thread will
                     * do it.
                     */
                    if (mustDie())
                        break;
                }

                /*
                 * This is the case if the current thread did no processing at
                 * all. In that case it should die.
                 */
                if (tlocalEvents == null) {
                    die();
                    break;
                }

                /**
                 * Simple rule - if someone voted that the VM must be resumed
                 * then it is resumed.
                 */
                /**
                 * This semaphore is necessary because there could be a case
                 * where another thread passes through here right after the
                 * thread processing the last event is done with it. The net
                 * result could be an EventSet being resumed twice.
                 */
                /*
                 * We could increase paralelism by adding another semaphore to
                 * handle these counter updates, but I don't think it's
                 * necessary since we shouldn't really have a lot of threads.
                 */
                sema.p();
                Integer counter = (Integer) event2int.get(tlocalEvents);
                if (pcm.getProcessingContext().getResults(
                        IEventManager.RESUME_SET) > 0
                        && counter.intValue() == 0) {
                    /** Ensures the EventSet won't be resumed twice.*/
                    event2int.put(tlocalEvents, new Integer(-1)); 

//                    try{
//                        resume(tlocalEvents);
//                    }catch(TargetRequestFailedException ex){
//                        mh.getErrorOutput().println("Failed to resume one or more" +
//                                " threads.");
//                    }
                    if(logger.isDebugEnabled())
                        logger.info("Resuming event set with " + tlocalEvents.size() + " events.");
                    tlocalEvents.resume();
                    /*
                     * Ideally this sema.v() should be before tlocalEvent.resume(),
                     * but since we might have a race condition in JDI we'd better
                     * not to risk it.
                     */
                    sema.v();
                } else {
                    sema.v();
                }

                if (mustDie())
                    break;

            }
        }catch(InterruptedException ex){
            logger.error(ex);
        } finally {
            /** If the thread got this far then it's going to die. */
            markToDie.add(Thread.currentThread());
            die();
        }
    }
    
    /** Resumes the event set using the same suspend policy dependant semantics 
     * described in the documentation of EventSet.  
     * 
     * @param eSet
     */
    private void resume(EventSet eSet) throws TargetRequestFailedException {
        int suspendPolicy = eSet.suspendPolicy();

        if (suspendPolicy == EventRequest.SUSPEND_NONE)
            return;
        else if (suspendPolicy == EventRequest.SUSPEND_ALL) {
            VirtualMachineManager vmm = this.getVMMByVM(eSet.virtualMachine());
            vmm.resumeInternal();
        } 
        
        else if (suspendPolicy == EventRequest.SUSPEND_EVENT_THREAD) {
            /** First we build a set of all suspended threads. */
            Set<ThreadReference> sThreads = new HashSet<ThreadReference>();
            Iterator<Event> it = eSet.eventIterator();
            
            while(it.hasNext()){
                Event evt = it.next();
                ThreadReference tr = nonReflectiveGetThread(evt);
                /** For some reason, the EventSetImpl class of jdt checks 
                 * for nullity here. They actually do a full VM resume in
                 * case they find a null value. Got to explore this in 
                 * more depth to see if there's a caveat somewhere.
                 */
                if(tr != null) sThreads.add(tr);
            }
            
            /** Now that we added all threads, we get their Eclipse 
             * representation and resume them through there. 
             */
            for(ThreadReference tr : sThreads){
                IJavaNodeManager vmm = getVMMByVM(tr.virtualMachine());
                try{
                    vmm.getThreadManager().getAdapterForThread(tr).resume();
                }catch(Exception ex){
                    throw new TargetRequestFailedException(ex);
                }
            }
        }
    }
    
    private VirtualMachineManager getVMMByVM(VirtualMachine vm)
        throws TargetRequestFailedException
    {
        IJavaNodeManager jnm = vmmf.getJavaNodeManager(vm);
        if(jnm == null)
            throw new TargetRequestFailedException("Virtual machine GID is invalid or has expired.");
        
        return (VirtualMachineManager)jnm.getAdapter(VirtualMachineManager.class);
    }
    
    private ThreadReference nonReflectiveGetThread(Event e){
        /** Does RTTI to avoid reflection. */
        if(e instanceof LocatableEvent) return ((LocatableEvent)e).thread();
        if(e instanceof ClassPrepareEvent) return ((ClassPrepareEvent)e).thread();
        if(e instanceof ThreadStartEvent) return ((ThreadStartEvent)e).thread();
        if(e instanceof ThreadDeathEvent) return ((ThreadDeathEvent)e).thread();
        if(e instanceof VMStartEvent) return ((VMStartEvent)e).thread();
        
        return null;
    }
    
    private synchronized boolean mustDie(){
        return markToDie.contains(Thread.currentThread());
    }
    
    private synchronized void die(){
        Thread c = Thread.currentThread();
        if(markToDie.contains(c)) markToDie.remove(Thread.currentThread());
        //mh.getDebugOutput().println("Thread " + c.getName() + " called die().");
        handlerThreads--;
        assert handlerThreads >= 0;
    }
    
    private synchronized void live(){
        /*
         * Some events are not meant to be resumed automatically. This registers
         * our current thread with the voting system.
         */
        pcm.register(new ProcessingContextImpl());
        takesNew = false;
    }
    
    public synchronized void handleNext(){
        if(handlerThreads == MAX_SIMULTANEOUS_EVENTS)
            throw new RuntimeException("You cannot start another handler thread - maximum handling" +
                    "capacity achieved.");
        
        handlerThreads++;
        
        Thread t = new Thread(new Runnable(){
            public void run() {
                doRun();
            }
        });
        
        t.setName("<" + jvmid + "> JDI Dispatcher " + (handlerThreads-1));
        
        /* Current handler thread will die when it returns */
        if(owner != null) markToDie.add(owner);
        
        /* We become the current handler thread */
        owner = t;
        t.start();
    }
    
    private void processEvent(Event e){
        
        MessageHandler mh = MessageHandler.getInstance();
        
        /** The dispatcher gets a chance of processing events before any other
         * handler.
         */
        this.localProcess(e);
        
        String suffix = "";
        
        try{
            /* Maybe I'm a bit off track here, but it seems to me that it's more
               elegant to use reflection than RTTI in this particular case.*/ 
            Class handlerClass = handler.getClass();
            Class evClass = e.getClass();
            
            Class [] intf = evClass.getInterfaces();
            
            Method process = null;
            
            /* Attempts to resolve method signature for all implemented
             * interfaces. This has a direct implication that only classes
             * implementing the event interfaces directly will work 
             * correctly.
             * 
             * Note: Fortunately this loop rarely goes through more than one
             * iteraction.
             */
            for(int i = 0; i < intf.length; i++){
                try{
                    Class [] param = { intf[i] };
                    String name = intf[i].getName();
                    int idx = name.lastIndexOf('.');
                    if(idx != -1)
                        suffix = name.substring(idx + 1);
                    process =
                        handlerClass.getDeclaredMethod("handle" + suffix, param);
                } catch(NoSuchMethodException ex) { }
            }
            
            // If we couldn't find it, try one more time.
            if(process == null){
                String name = evClass.getName();
                int idx = name.lastIndexOf('.');
                if(idx != -1)
                    suffix = name.substring(idx + 1);

                Class [] param = { evClass };
                process =
                    handlerClass.getDeclaredMethod("handle" + suffix, param);
            }
            
            process.invoke(handler, new Object[] { e });
        /**
         * The exceptions bellow shouldn't happen if we use the EventHandler
         * interface, however, it might be the case that we change the interface, 
         * so I've decided to handle these errors anyway.
         */
        } catch (NoSuchMethodException ex) {
            logger.error(" Registered handler does not define handle"
                            + suffix, ex);
        } catch (IllegalAccessException ex) {
            logger.error(" Error accessing handle" + suffix
                            + ". It has probably been declared as private",ex);
        } catch (InvocationTargetException ex) {
            logger.error(" Caught exception during invocation",ex);
        }
    }
    
    protected void localProcess(Event e){
        if(e instanceof VMDisconnectEvent || e instanceof VMDeathEvent){
            logger.info(" JVM " + jvmid + " terminated. Shutting down.");
            connected = false;
        }
    }
    
    public boolean isConnected(){
        return connected;
    }
    
    public void setDebugContext(IDebugContext dc){
        this.dc = dc;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IVotingManager#registerVoteType(java.lang.String)
     */
    public void declareVoterFor(String type) {
    		synchronized(voteSet){
    			voteSet.add(type);
    		}
    }
    
    private boolean existsVoteType(String type){
    		synchronized(voteSet){
    			return voteSet.contains(type);
    		}
    }
    
    private class ProcessingContextImpl implements IProcessingContext{

        private Map <String, Integer> votes = new HashMap <String, Integer>();
        private boolean processing = false;
                
        public void vote(String vote){
            Integer cv = checkupGet(vote, true);
            votes.put(vote, new Integer(cv.intValue()+1));
        }
        
        public synchronized int getResults(String vote){
            Integer cv = checkupGet(vote, false);
            return cv.intValue();
        }
        
        private Integer checkupGet(String vote, boolean active)
        {
            if(!existsVoteType(vote))
            	throw new NoContextException("Invalid vote type.");
            
            Integer v = votes.get(vote);
            if(v == null){
                v = new Integer(0);
                votes.put(vote, v);
            }
            
            /* The "active" flag is a sort of a workaround. Without this flag
             * the event dispatcher would get an exception when it
             * tries to get the election results after the voting session
             * (processing chain) has already espired (terminated).*/
            if(processing == false && active){
                throw new IllegalStateException("You cannot vote " +
                		"while there's no event being processed.");
            }
            
            return v;
        }
        
        private void setProcessing(boolean what){
            processing = what;
        }
        
        private void reset(){
            votes.clear();
        }
    }

}