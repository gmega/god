/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: EventHandlerImpl.java
 */

package ddproto1.debugger.eventhandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequest;

import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.exception.ConfigException;
import ddproto1.exception.InternalError;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.MultiMap;
import ddproto1.util.collection.OrderedMultiMap;
import ddproto1.util.collection.UnorderedMultiMap;

/**
 * This class represents the standard event handler. It's called DelegatingHandler
 * because it "delegates" event processing to its event processor chains (creative, huh? :-).
 * 
 * It's been conceived with concurrency in mind. 
 * 
 * @author giuliano
 *
 */
public class DelegatingHandler implements IEventHandler, IEventManager {

    private static final Logger logger = MessageHandler.getInstance().getLogger(DelegatingHandler.class);
    private static final Object PROCESSING_MARK = new Object();

    private final LockableDispatchTable<Integer, IJDIEventProcessor> listenersByType = 
        new LockableDispatchTable<Integer, IJDIEventProcessor>(
            new OrderedMultiMap<Integer, IJDIEventProcessor>(LinkedList.class));

    private final LockableDispatchTable<EventRequest, IJDIEventProcessor> listenersByRequest = 
        new LockableDispatchTable<EventRequest, IJDIEventProcessor>(
            new UnorderedMultiMap<EventRequest, IJDIEventProcessor>(
                    HashSet.class));

    private final Map<IJDIEventProcessor, Set<Integer>> listeners2policies = 
        new ConcurrentHashMap<IJDIEventProcessor, Set<Integer>>();

    private final ThreadLocal<List<Runnable>> deferredCommands = new ThreadLocal<List<Runnable>>();   

    private volatile List<Logger> eventLoggers;

    public DelegatingHandler() {
        initLoggers();
    }

    private void initLoggers() {
        MessageHandler mh = MessageHandler.getInstance();
        List<Logger> lList = new ArrayList<Logger>(IEventManager.N_TYPES);
        for (int i = 0; i < IEventManager.N_TYPES; i++) {
            String loggerName = DelegatingHandler.class.getName() + "."
                    + eventtypes[i];
            Logger addedLogger = mh.getLogger(loggerName);
            addedLogger.debug("Logger added");
            lList.add(i, mh.getLogger(loggerName));
        }
        eventLoggers = lList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMStartEvent(com.sun.jdi.event.VMStartEvent)
     */
    public void handleVMStartEvent(VMStartEvent e) {
        broadcast(VM_START_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMDeathEvent(com.sun.jdi.event.VMDeathEvent)
     */
    public void handleVMDeathEvent(VMDeathEvent e) {
        broadcast(VM_DEATH_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.EventHandler#handleBreakpointEvent(com.sun.jdi.event.BreakpointEvent)
     */
    public void handleBreakpointEvent(BreakpointEvent e) {
        broadcast(BREAKPOINT_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.EventHandler#handleClassPrepareEvent(com.sun.jdi.event.ClassPrepareEvent)
     */
    public void handleClassPrepareEvent(ClassPrepareEvent e) {
        broadcast(CLASSPREPARE_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMDisconnectEvent(com.sun.jdi.event.VMDisconnectEvent)
     */
    public void handleVMDisconnectEvent(VMDisconnectEvent e) {
        broadcast(VM_DISCONNECT_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleExceptionEvent(com.sun.jdi.event.ExceptionEvent)
     */
    public void handleExceptionEvent(ExceptionEvent e) {
        broadcast(EXCEPTION_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleStepEvent(com.sun.jdi.event.StepEvent)
     */
    public void handleStepEvent(StepEvent e) {
        broadcast(STEP_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleThreadStartEvent(com.sun.jdi.event.ThreadStartEvent)
     */
    public void handleThreadStartEvent(ThreadStartEvent e) {
        broadcast(THREAD_START_EVENT, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleThreadDeathEvent(com.sun.jdi.event.ThreadDeathEvent)
     */
    public void handleThreadDeathEvent(ThreadDeathEvent e) {
        broadcast(THREAD_DEATH_EVENT, e);
    }

    /**
     * <b> Note: </b> This implementation guarantees listeners added first will
     * be called first.
     * 
     * @see IEventManager#addEventListener(int, IEventProcessor)
     * 
     */
    public void addEventListener(final int type,
            final IJDIEventProcessor listener) throws IllegalAttributeException {

        if (((type < 0) || (type >= N_TYPES)) && type != ALL)
            throw new IllegalAttributeException(" Invalid event type specified.");
        
        if (listenersByType.isProcessingThread()) {
            this.scheduleCommand(new Runnable() {
                public void run() {
                    try {
                        addEventListener(type, listener);
                    } catch (IllegalAttributeException ex) {
                        // Won't happen. 
                        throw new InternalError("Unexpected exception thrown.",
                                ex);
                    }
                }
            });

            return;
        }

        try {
            listenersByType.acquireWriteLock();

            OrderedMultiMap<Integer, IJDIEventProcessor> listeners = (OrderedMultiMap) listenersByType
                    .getDispatchTable();

            if (type == ALL) {
                for (int i = 0; i < N_TYPES; i++)
                    listeners.add(i, listener);
                return;
            }
            listeners.addLast(type, listener);

        } finally {
            listenersByType.releaseWriteLock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.eventhandler.IEventProducer#setListenerPolicyFilters(IEventProcessor,
     *      Set)
     */
    public void setListenerPolicyFilters(final IJDIEventProcessor listener,
            final Set<Integer> policyFilters) throws ConfigException {
        if (listenersByType.isProcessingThread())
            throw new IllegalStateException(
                    "Processing thread cannot change filters.");

        try {
            listenersByType.acquireWriteLock();
            /*
             * Rather expensive verification - fortunately, listener policy
             * filters are rarely changed after they're first set.
             */
            boolean found = false;
            for (int i = 0; i < N_TYPES; i++) {
                if (listenersByType.getDispatchTable().contains(i, listener)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new ConfigException(" You cannot set policy filters "
                        + "for an unregistered listener");
            }

            if (!(policyFilters == null))
                listeners2policies.put(listener, new HashSet<Integer>(
                        policyFilters));
        } finally {
            listenersByType.releaseWriteLock();
        }
    }

    public void removeEventListener(final int type,
            final IJDIEventProcessor listener) throws IllegalAttributeException {

        if ((type < 0) || (type >= N_TYPES))
            throw new IllegalAttributeException("Invalid event type specified.");

        if (listenersByType.isProcessingThread()) {
            this.scheduleCommand(new Runnable() {
                public void run() {
                    try {
                        removeEventListener(type, listener);
                    } catch (IllegalAttributeException ex) {
                        throw new InternalError("Unexpected exception thrown.",
                                ex);
                    }
                }
            });

            return;
        }

        try {
            listenersByType.acquireWriteLock();

            MultiMap<Integer, IJDIEventProcessor> table = listenersByType.getDispatchTable();

            if (type == ALL) {
                for (int i = 0; i < N_TYPES; i++)
                    table.remove(i, listener);
                return;
            }

            table.remove(type, listener);
            listeners2policies.remove(listener);
        } finally {
            listenersByType.releaseWriteLock();
        }
    }

    private void scheduleCommand(Runnable r) {
        List <Runnable> deferred = (List <Runnable>)deferredCommands.get();
        if(deferred == null){
            deferred = new ArrayList<Runnable>(5);
            deferredCommands.set(deferred);
        }
        deferred.add(r);
    }

    private void performDelayed() {
        Iterable<Runnable> delayeds = deferredCommands.get();
        deferredCommands.remove();
        
        if (delayeds == null)
            return;
        
        for (Runnable delayed : delayeds){
            try{
                delayed.run();
            }catch(Throwable t){
                logger.error("Error while processing deferred command.", t);
            }
        }
    }

    private void broadcast(int typeIndex, Event e) {
        if (eventLoggers.get(typeIndex).isDebugEnabled()) {
            // Dammit, I hate doing this, but ClassPrepareEvent.toString sucks!
            if (typeIndex == CLASSPREPARE_EVENT) {
                ReferenceType rt = ((ClassPrepareEvent) e).referenceType();
                eventLoggers.get(typeIndex).debug(
                        "ClassPrepareEvent: " + rt.toString());
            }else{
                eventLoggers.get(typeIndex).debug("Event: " + e.toString());
            }
        }

        broadcastToEventListeners(typeIndex, e);
        broadcastToRequestListeners(e);
    }

    private void broadcastToRequestListeners(Event e) {
        try {
            listenersByRequest.acquireReadLock();
            listenersByRequest.addProcessingMark();

            Iterable<IJDIEventProcessor> procs = listenersByRequest
                    .getDispatchTable().get(e.request());
            if (procs == null)
                return;
            /**
             * Don't need to check if isEligible since we don't allow policy
             * filters to be set for event request listeners (it doesn't make
             * sense).
             */
            for (IJDIEventProcessor proc : procs){
                try{
                    proc.process(e);
                }catch(Exception ex){
                    logger.error("Error while processing event", ex);
                }
            }
        } finally {
            listenersByRequest.removeProcessingMark();
            listenersByRequest.releaseReadLock();
            this.performDelayed();
        }
    }

    private void broadcastToEventListeners(int typeIndex, Event e) {
        try {
            listenersByType.acquireReadLock();
            listenersByType.addProcessingMark();

            Iterable<IJDIEventProcessor> l = listenersByType.getDispatchTable()
                    .get(typeIndex);

            if (l == null)
                return;

            for (IJDIEventProcessor el : l) {
                if (isEligible(el, e)) {
                    try {
                        el.process(e);
                    } catch (Exception ex) {
                        logger.error("Error while processing event", ex);
                    }
                }
            }

        } finally {
            listenersByType.removeProcessingMark();
            listenersByType.releaseReadLock();
            this.performDelayed();
        }
    }

    private boolean isEligible(IJDIEventProcessor iep, Event e) {
        EventRequest original = e.request();
        Set policySet = (Set) listeners2policies.get(iep);
        if (policySet == null)
            return true;
        // Case when the policy is undefined (we have no request).
        // TODO Actually I have no idea of what to do in this particular case.
        if (original == null) {
            if (policySet.contains(NULL_POLICY))
                return true;
        } else if (policySet.contains(new Integer(original.suspendPolicy()))) {
            return true;
        }

        return false;
    }

    public void addEventListener(final EventRequest request,
            final IJDIEventProcessor listener) {

        if (listenersByRequest.isProcessingThread()) {
            this.scheduleCommand(new Runnable() {
                public void run() {
                    addEventListener(request, listener);
                }
            });
            return;
        }

        try {
            listenersByRequest.acquireWriteLock();
            listenersByRequest.getDispatchTable().add(request, listener);
        } finally {
            listenersByRequest.releaseWriteLock();
        }
    }

    public void removeEventListener(final EventRequest request,
            final IJDIEventProcessor listener) {

        if (listenersByRequest.isProcessingThread()) {
            this.scheduleCommand(new Runnable() {
                public void run() {
                    removeEventListener(request, listener);
                }
            });
            return;
        }

        try {
            listenersByRequest.acquireWriteLock();
            listenersByRequest.getDispatchTable().remove(request, listener);
        } finally {
            listenersByRequest.releaseWriteLock();
        }
    }

    private class LockableDispatchTable<K, V> {

        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        private Lock rLock = rwLock.readLock();
        private Lock wLock = rwLock.writeLock();

        private ThreadLocal<Object> processingMarks = new ThreadLocal<Object>();

        private MultiMap<K, V> actualTable;

        public LockableDispatchTable(MultiMap mm) {
            this.actualTable = mm;
        }

        public MultiMap getDispatchTable() {
            return actualTable;
        }

        public void addProcessingMark() {
            processingMarks.set(PROCESSING_MARK);
        }

        public void removeProcessingMark() {
            processingMarks.set(null);
        }

        public boolean isProcessingThread() {
            return processingMarks.get() == PROCESSING_MARK;
        }

        public void acquireReadLock() {
            rLock.lock();
        }

        public void releaseReadLock() {
            rLock.unlock();
        }

        public void acquireWriteLock() {
            wLock.lock();
        }

        public void releaseWriteLock() {
            wLock.unlock();
        }
    }
}