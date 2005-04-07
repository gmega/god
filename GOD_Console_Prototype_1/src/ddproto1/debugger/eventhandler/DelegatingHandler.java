/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: EventHandlerImpl.java
 */

package ddproto1.debugger.eventhandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;

/**
 * @author giuliano
 *
 */
public class DelegatingHandler implements IEventHandler, IEventManager{
    
    private static final String module = "DelegatingHandler -";
    
    DeferrableRequestQueue pending;
   
    private static final int N_TYPES = 9;
    
    private List[] listeners;
    private Map listeners2policies;
    
    public DelegatingHandler() {
        listeners = new List[N_TYPES];
        for(int i = 0; i < N_TYPES; i++)
            listeners[i] = new LinkedList();
        
        listeners2policies = new HashMap();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMStartEvent(com.sun.jdi.event.VMStartEvent)
     */
    public void handleVMStartEvent(VMStartEvent e) {
        broadcast(listeners[VM_START_EVENT], e);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMDeathEvent(com.sun.jdi.event.VMDeathEvent)
     */
    public void handleVMDeathEvent(VMDeathEvent e) {
        broadcast(listeners[VM_DEATH_EVENT], e);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventHandler#handleBreakpointEvent(com.sun.jdi.event.BreakpointEvent)
     */
    public void handleBreakpointEvent(BreakpointEvent e) {
        broadcast(listeners[BREAKPOINT_EVENT], e);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventHandler#handleClassPrepareEvent(com.sun.jdi.event.ClassPrepareEvent)
     */
    public void handleClassPrepareEvent(ClassPrepareEvent e) {
    	broadcast(listeners[CLASSPREPARE_EVENT], e);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventHandler#handleVMDisconnectEvent(com.sun.jdi.event.VMDisconnectEvent)
     */
    public void handleVMDisconnectEvent(VMDisconnectEvent e) {
        broadcast(listeners[VM_DISCONNECT_EVENT], e);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleExceptionEvent(com.sun.jdi.event.ExceptionEvent)
     */
    public void handleExceptionEvent(ExceptionEvent e) {
        broadcast(listeners[EXCEPTION_EVENT], e);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleStepEvent(com.sun.jdi.event.StepEvent)
     */
    public void handleStepEvent(StepEvent e) {
        broadcast(listeners[STEP_EVENT], e);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleThreadStartEvent(com.sun.jdi.event.ThreadStartEvent)
     */
    public void handleThreadStartEvent(ThreadStartEvent e) {
        broadcast(listeners[THREAD_START_EVENT], e);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IEventHandler#handleThreadDeathEvent(com.sun.jdi.event.ThreadDeathEvent)
     */
    public void handleThreadDeathEvent(ThreadDeathEvent e) {
        broadcast(listeners[THREAD_DEATH_EVENT], e);
    }
    
    /** 
     * <b> Note: </b> This implementation guarantees listeners added
     * first will be called first.
     * 
     * @see IEventManager#addEventListener(int, IEventProcessor)
     * 
     */
    public void addEventListener(int type, IJDIEventProcessor listener)
            throws IllegalAttributeException {
        
        if (type == ALL){
            for(int i = 0; i < N_TYPES; i++)
                listeners[i].add(listener);
            
            return;
        }
        
        if ((type < 0) || (type >= N_TYPES))
            throw new IllegalAttributeException(module
                    + " Invalid event type specified.");

        listeners[type].add(listeners[type].size(), listener);
    }
    
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.IEventProducer#setListenerPolicyFilters(IEventProcessor, Set)
     */
     public void setListenerPolicyFilters(IJDIEventProcessor listener, Set policyFilters)
    	throws ConfigException
    {
        /* Rather expensive verification - fortunately, listener policy filters 
         * are rarely changed after they're first set. */
        boolean found = false;
        for(int i = 0; i < N_TYPES; i++){
            if(listeners[i].contains(listener)) { found = true; break; }
        }
        
        if (!found){
            throw new ConfigException(module
                    + " You cannot set policy filters "
                    + "for an unregistered listener");
        }

        if(!(policyFilters == null))
            listeners2policies.put(listener, policyFilters);
    }

    public void removeEventListener(int type, IJDIEventProcessor listener)
            throws IllegalAttributeException {
        
        if (type == ALL){
            for(int i = 0; i < N_TYPES; i++)
                listeners[i].remove(listener);
            
            return;
        }

        if ((type < 0) || (type >= N_TYPES))
            throw new IllegalAttributeException(module
                    + " Invalid event type specified.");

        listeners[type].remove(listener);
        listeners2policies.remove(listener);
    }
    
    private void broadcast(List l, Event e){
        Iterator it = l.iterator();
        
        while(it.hasNext()){
            IJDIEventProcessor el = (IJDIEventProcessor)it.next();
            if(isEligible(el, e))
                el.process(e);
        }
    }
    
    private boolean isEligible(IJDIEventProcessor iep, Event e){
        EventRequest original = e.request();
        Set policySet = (Set)listeners2policies.get(iep);
        if(policySet == null) return true;
        // Case when the policy is undefined (we have no request).
        // TODO Actually I have no idea of what to do in this particular case.
        if(original == null){
            if(policySet.contains(NULL_POLICY))
                return true;
        }else if(policySet.contains(new Integer(original.suspendPolicy()))){
            return true;
        }
        
        return false;
    }
}