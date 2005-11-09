package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractDeferrableRequest implements IDeferrableRequest{

	private List<IPrecondition> preconditions = new ArrayList<IPrecondition>();
	private List<IResolutionListener> listeners = new ArrayList<IResolutionListener>();
	
	private boolean cancelled = false;
		
	protected void addRequirement(IPrecondition precondition){
		preconditions.add(precondition);
	}
	
	public Object resolveNow(IResolutionContext ctx) throws Exception{
		if(cancelled) throw new IllegalStateException("Cannot resolve " +
				" a cancelled request.");
		return this.resolveInternal(ctx);
	}
	
	protected abstract Object resolveInternal(IResolutionContext ctx) throws Exception;
	
	
	public void cancel(){
		if(cancelled) throw new IllegalStateException("Cannot cancel a cancelled" +
				" request.");
		this.cancelInternal();
		cancelled = true;
	}
	
	protected abstract void cancelInternal();
		
	protected void broadcastToListeners(Object what){
		for(IResolutionListener resListener : listeners)
			resListener.notifyResolution(this, what);
	}
		
	public List<IPrecondition> getRequirements() {
		return Collections.unmodifiableList(preconditions);
	}

	public void addResolutionListener(IResolutionListener listener) {
		listeners.add(listener);
	}

	public void removeResolutionListener(IResolutionListener listener) {
		listeners.remove(listener);
	}
}
