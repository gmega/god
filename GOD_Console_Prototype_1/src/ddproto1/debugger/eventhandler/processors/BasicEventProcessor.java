/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;

import ddproto1.debugger.managing.IDebugContext;

/**
 * Skeleton for Event Processors. It provides default implementations
 * for methods like setNext, enable, etc. and a skeleton <b>process</b> method
 * that tries to eliminate recursion. The user of this class should override
 * <b>specializedProcess</b>.
 * 
 * @author giuliano
 * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor
 *
 */
public abstract class BasicEventProcessor implements IJDIEventProcessor {
    
    private IJDIEventProcessor next = null;
    private boolean enabled = true;
    protected IDebugContext dc = null;
    
    public void process(Event e){
        specializedProcess(e);
        if(next == null) return;
        
        // This eliminates recursion when possible
        for(IJDIEventProcessor el = next; el != null; el = (BasicEventProcessor)el.getNext()){
            if(el instanceof BasicEventProcessor){
                BasicEventProcessor ep = (BasicEventProcessor)el;
                if(ep.enabled)
                    ep.specializedProcess(e);
            }else{
                el.process(e);
                break;
            }
        }
    }
    
    public void setNext(IJDIEventProcessor el){
        next = (BasicEventProcessor)el;
    }
    
    public IJDIEventProcessor getNext(){
        return next;
    }
    
    public void enable(boolean stats){
        enabled = stats;
    }
    
    public void setDebugContext(IDebugContext dc){
        this.dc = dc;
    }
    
    protected abstract void specializedProcess(Event e);
   
}
