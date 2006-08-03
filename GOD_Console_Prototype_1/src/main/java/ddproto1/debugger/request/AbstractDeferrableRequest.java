package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.commons.UnsupportedException;

public abstract class AbstractDeferrableRequest implements IDeferrableRequest{

    private static IVMManagerFactory vmmf = VMManagerFactory.getInstance();
    
	private List<IPrecondition> preconditions = new ArrayList<IPrecondition>();
	private List<IResolutionListener> listeners = new ArrayList<IResolutionListener>();
	
	private boolean cancelled = false;
    
    private String vmid;
		
    protected AbstractDeferrableRequest(String vmid){
        this.vmid = vmid;
    }
    
	protected void addRequirement(IPrecondition precondition){
		preconditions.add(precondition);
	}
	
	public Object resolveNow(IResolutionContext ctx) throws Exception{
		if(cancelled) throw new IllegalStateException("Cannot resolve " +
				" a cancelled request.");
		return this.resolveInternal(ctx);
	}
	
	protected abstract Object resolveInternal(IResolutionContext ctx) throws Exception;
		
	public synchronized void cancel() throws Exception{
		if(cancelled) throw new IllegalStateException("Cannot cancel a cancelled" +
				" request.");
		this.cancelInternal();
		cancelled = true;
	}
    
    public boolean isCancelled(){
        return cancelled;
    }
	
	protected abstract void cancelInternal() throws Exception;
		
	protected void broadcastToListeners(Object what){
        broadcastToListeners(this, what);
	}
    
    protected void broadcastToListeners(IDeferrableRequest source, Object what){
        for(IResolutionListener resListener : listeners)
            resListener.notifyResolution(source, what);
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
    
    protected IJavaNodeManager getVMM(){
        if(vmid == null) throw new UnsupportedException("Unsupported operation for this type " +
                "of deferrable request");
        return (IJavaNodeManager)vmmf.getNodeManager(vmid).getAdapter(IJavaNodeManager.class);
    }
    
    public String getVMID(){
        return vmid;
    }
}
