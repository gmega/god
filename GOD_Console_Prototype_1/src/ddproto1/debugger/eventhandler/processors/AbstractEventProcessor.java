/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;

import ddproto1.debugger.managing.IDebugContext;
import ddproto1.util.traits.JDIEventProcessorTrait;
import ddproto1.util.traits.JDIEventProcessorTrait.JDIEventProcessorTraitImplementor;

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
public abstract class AbstractEventProcessor implements IJDIEventProcessor, JDIEventProcessorTraitImplementor {
    
    private IJDIEventProcessor next = null;
    protected boolean enabled = true;
    protected IDebugContext dc = null;
    
    private JDIEventProcessorTrait jdiTrait = new JDIEventProcessorTrait(this);
    
    public void process(Event e){
    	jdiTrait.process(e);
    }
    
    public void setNext(IJDIEventProcessor el){
    	jdiTrait.setNext(el);
    }
    
    public IJDIEventProcessor getNext(){
    	return jdiTrait.getNext();
    }
    
    public void enable(boolean stats){
    	jdiTrait.enable(stats);
    }
    
    public void setDebugContext(IDebugContext dc){
        this.dc = dc;
    }

	public boolean enabled() {
		return enabled;
	}

	public void enabled(boolean newValue) {
		enabled = newValue;
	}

	public IJDIEventProcessor next() {
		return next;
	}

	public void next(IJDIEventProcessor next) {
		this.next = next;
	}
   
}
