package ddproto1.debugger.managing;

import org.eclipse.debug.core.model.Breakpoint;

import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.IDeferrableRequest;

public class JavaBreakpoint extends Breakpoint {

	private String typeName;
	private int    line;
	
	public String getModelIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void addToTarget(IJavaDebugTarget target){
		VirtualMachineManager vmm = target.getVMManager();
		DeferrableBreakpointRequest dbr = 
			new DeferrableBreakpointRequest(vmm.getName(), typeName, line);
		vmm.getDeferrableRequestQueue().addEagerlyResolve(dbr);
	}

}
