/*
 * Created on Nov 7, 2005
 * 
 * file: IJavaDebugTarget.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.model.IDebugTarget;

public interface IJavaDebugTarget extends IDebugTarget{
    public VirtualMachineManager getVMManager();
    public boolean canTerminate();
    
    public void bindTo(VirtualMachineManager vmm);
    
    /** Adapter layer for events. */
    public void handleDeath();
    public void handleDisconnect();
    public void handleSuspend(int detail);
    public void handleResume(int detail);
    
}
