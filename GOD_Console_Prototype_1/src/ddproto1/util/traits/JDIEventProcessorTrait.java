package ddproto1.util.traits;

import com.sun.jdi.event.Event;

import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;

public class JDIEventProcessorTrait implements IJDIEventProcessor{
	
	public interface JDIEventProcessorTraitImplementor{
		public void specializedProcess(Event request);
		public void next(IJDIEventProcessor next);
		public IJDIEventProcessor next();
		public boolean enabled();
		public void enabled(boolean newValue);
	}
	
	private JDIEventProcessorTraitImplementor _this;
	
	public JDIEventProcessorTrait(JDIEventProcessorTraitImplementor implementor){
		_this = implementor;
	}
	
	public void process(Event request){
        if(_this.enabled())
            _this.specializedProcess(request);
        if(_this.next() == null) return;
        
        // This eliminates recursion when possible
        for(IJDIEventProcessor el = _this.next(); el != null; el = (AbstractEventProcessor)el.getNext()){
            if(el instanceof JDIEventProcessorTraitImplementor){
                JDIEventProcessorTraitImplementor ep = (JDIEventProcessorTraitImplementor)el;
                if(ep.enabled())
                    ep.specializedProcess(request);
            }else{
                el.process(request);
                break;
            }
        }
	}

	public void setNext(IJDIEventProcessor iep) {
		_this.next(iep);
	}

	public IJDIEventProcessor getNext() {
		return _this.next();
	}

	public void enable(boolean status) {
		_this.enabled(status);
	}

}
