/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SuspendPolicyManager.java
 */

package ddproto1.util;

import com.sun.jdi.request.EventRequest;

import ddproto1.exception.UnsupportedException;

/**
 * @author giuliano
 *
 */
public class PolicyManager {
    private static final String module = "PolicyManager -";
    private static PolicyManager instance = null;
    
    private PolicyManager() { };
    
    public synchronized static PolicyManager getInstance(){
        if(instance == null)
            instance = new PolicyManager();
        
        return instance;
    }
    
    public int getPolicy(String eventType){
        
        if(eventType.equals("request.breakpoint") || eventType.equals("request.step")){
            return EventRequest.SUSPEND_EVENT_THREAD;
        }
        
        if(eventType.startsWith("request.")){
            return(EventRequest.SUSPEND_ALL);
        }
        
        throw new UnsupportedException(module + " Unsupported policy " + eventType);
    }
    
}
