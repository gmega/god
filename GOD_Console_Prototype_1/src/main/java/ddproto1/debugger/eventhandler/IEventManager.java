/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IEventProducer.java
 */

package ddproto1.debugger.eventhandler;

import java.util.Set;

import com.sun.jdi.request.EventRequest;

import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.IllegalAttributeException;

/**
 * This interface describes the event subscription interface
 * exposed by the Java node manager.
 * 
 * Note about removal methods:
 * Registered listeners should be aware that removing a listener
 * with removeEventListener does not guarantee that no other events
 * will be delivered to that listener. There could be a thread waiting
 * to perform delivery (contending for a listener lock). Also, listeners
 * that attempt to modify the dispatch table from a dispatch thread will
 * have their modifications delayed. 
 * 
 * @author giuliano
 *
 */
public interface IEventManager {
    
    public static final String RESUME_SET = "RESUME_SET";
    public static final String NO_SOURCE = "NO_SOURCE";
    
    public static final int VM_START_EVENT = 0;
    public static final int VM_DEATH_EVENT = 1;
    public static final int VM_DISCONNECT_EVENT = 2;
    public static final int BREAKPOINT_EVENT = 3;
    public static final int CLASSPREPARE_EVENT = 4;
    public static final int EXCEPTION_EVENT = 5;
    public static final int STEP_EVENT = 6;
    public static final int THREAD_START_EVENT = 7;
    public static final int THREAD_DEATH_EVENT = 8;
    
    public static final String [] eventtypes = 
    	new String [] {
    		"VM_START_EVENT"  , "VM_DEATH_EVENT"    , "VM_DISCONNECT_EVENT", 
    		"BREAKPOINT_EVENT", "CLASSPREPARE_EVENT", "EXCEPTION_EVENT", 
    		"STEP_EVENT"      , "THREAD_START_EVENT", "THREAD_DEATH_EVENT"
    	};

    public static final int N_TYPES = 9;
    
    public static final int ALL = -1;
    
    /* Add this to your policy filters if you want to receive events
     * that have no associated suspend policy (those are pretty rare).
     */
    public static final Object NULL_POLICY = new Object();
    
    /** Register an event processor that will be notified whenever
     * an event with the required type is produced. Events might be: <BR>
     * <ol>
     *  <li><b>VM_START_EVENT</b> See com.sun.jdi.VMStartEvent</li>
     *  <li><b>VM_DEATH_EVENT</b> See com.sun.jdi.VMDeathEvent</li>
     *  <li><b>VM_DISCONNECT_EVENT</b> See com.sun.jdi.VMDisconnectEvent</li>
     *  <li><b>BREAKPOINT_EVENT</b> See com.sun.jdi.BreakpointEvent</li>
     *  <li><b>CLASSPREPARE_EVENT</b> See com.sun.jdi.ClassPrepareEvent</li>
     *  <li><b>EXCEPTION_EVENT</b> See com.sun.jdi.ExceptionEvent</li>
     *  <li><b>STEP_EVENT</b> See com.sun.jdi.StepEvent</li>
     *  <li><b>THREAD_START_EVENT</b> See com.sun.jdi.ThreadStartEvent</li>
     *  <li><b>THREAD_DEATH_EVENT</b> See com.sun.jdi.ThreadDeathEvent</li>
     *  <li><b>ALL</b> Listens to all events. </li>
     * </ol>
     * 
     * @param type Type of the event to listen to.
     * @param listener Listener to callback when event happens.
     * @throws IllegalAttributeException if an invalid event type is specified.
     */

    public void addEventListener(int type, IJDIEventProcessor listener) throws IllegalAttributeException;
    
    public void addEventListener(EventRequest request, IJDIEventProcessor listener);
        
    /** Removes a previously registered event listener (IEventProcessor).
     * 
     * @param type The event channel type from where to unregister the listener.
     * @param listener Listener to unregister
     * @throws IllegalAttributeException thrown if the event channel type is invalid
     */
    public void removeEventListener(int type, IJDIEventProcessor listener) throws IllegalAttributeException;
    
    public void removeEventListener(EventRequest request, IJDIEventProcessor listener);
    
    /** Restricts event delivery to those satisfying a given SuspendPolicy 
     * (see com.sun.jdi.EventRequest for a list of possible suspend policies).
     * Each listener can have only ONE associated policy set, even if that listener
     * gets multiple events. If you need different policy filters for the same 
     * listener on different event types, you can create an internal class that calls back
     * your listener and implements IEventProcessor, then register two instances of it. 
     * 
     * @param listener
     * @param policyFilters
     * @throws ConfigException If an invalid policy is supplied. Valid policies are those defined in com.sun.jdi.EventRequest.
     */
    public void setListenerPolicyFilters(IJDIEventProcessor listener, Set <Integer>policyFilters) throws ConfigException;
    
}
