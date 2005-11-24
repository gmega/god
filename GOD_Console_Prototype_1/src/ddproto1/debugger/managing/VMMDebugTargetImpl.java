/*
 * Created on Nov 22, 2005
 * 
 * file: VMMDebugTargetImpl.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import ddproto1.commons.DebuggerConstants;
import ddproto1.util.MessageHandler;

public class VMMDebugTargetImpl extends JavaDebugElement implements IJavaDebugTarget{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(VMMDebugTargetImpl.class);
    
    private ILaunch launch;
    
    private VirtualMachineManager vmm;
    
    private boolean supportsTerminate;
    
    private boolean supportsDisconnect;
    
    private boolean resumeOnStart;
    
    private IProcess process;
    
    private List<JavaBreakpoint> installedBreakpoints = new ArrayList<JavaBreakpoint>();
    
    public VMMDebugTargetImpl(ILaunch launch, boolean supportTerminate, 
            boolean supportDisconnect, IProcess process, boolean resume){
        super(null);
        setLaunch(launch);
        setSupportsTerminate(supportTerminate);
        setSupportsDisconnect(supportDisconnect);
        setProcess(process);
        setResumeOnStartup(resume);
        initializeBreakpoints();
        
        /** Starts the process. */
        doAsyncConnect();
    }
    
    public void bindTo(VirtualMachineManager vmm){
        if(this.getVMM() != null)
            throw new IllegalStateException("Cannot bind " +
                    "target to more than one Virtual Machine Manager.");
        this.setVMM(vmm);
    }
    
    protected void setLaunch(ILaunch launch) { this.launch = launch; }
    
    protected void setSupportsTerminate(boolean supportsTerminate){
        this.supportsTerminate = supportsTerminate;
    }

    protected void setVMM (VirtualMachineManager vmm) { this.vmm = vmm; }
    
    /**
     * @return Returns the vmm.
     */
    protected VirtualMachineManager getVMM() {
        return vmm;
    }

    /**
     * @param process The process to set.
     */
    protected void setProcess(IProcess process) {
        this.process = process;
    }

    /**
     * @param resumeOnStart The resumeOnStart to set.
     */
    protected void setResumeOnStartup(boolean resumeOnStart) {
        this.resumeOnStart = resumeOnStart;
    }

    /**
     * @param supportsDisconnect The supportsDisconnect to set.
     */
    protected void setSupportsDisconnect(boolean supportsDisconnect) {
        this.supportsDisconnect = supportsDisconnect;
    }

    public IProcess getProcess() {
        return process;
    }

    public IThread[] getThreads() throws DebugException {
        return vmm.getThreadManager().getThreads();
    }

    public boolean hasThreads() throws DebugException {
        return vmm.getThreadManager().hasThreads();
    }

    public String getName() throws DebugException {
        return vmm.getName();
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint instanceof JavaBreakpoint;
    }

    public boolean canTerminate() {
        return supportsTerminate() && vmm.isAvailable();
    }
    
    public void terminate() throws DebugException {
        vmm.terminate();
    }

    public boolean canResume() {
        return isSuspended() && vmm.isAvailable();
    }

    public boolean canSuspend() {
        return !isSuspended() && vmm.isAvailable();
    }
    
    private void initializeBreakpoints(){
        IBreakpointManager manager= DebugPlugin.getDefault().getBreakpointManager();
        manager.addBreakpointListener(this);
        IBreakpoint[] bps = manager.getBreakpoints(DebuggerConstants.PLUGIN_ID);
        for (int i = 0; i < bps.length; i++) {
            if (bps[i] instanceof JavaBreakpoint) {
                breakpointAdded(bps[i]);
            }
        }
    }
    
    private void doAsyncConnect(){
        Thread connector = new Thread(new Runnable(){
            public void run(){
                try{
                    vmm.connect();
                }catch(Exception ex){
                    logger.error("Failed to connect to target jvm.", ex);
                    handleDeath();
                }
            }
        });
        
        connector.start();
    }

    public void resume() throws DebugException {
        
    }

    public void suspend() throws DebugException {
        
    }

    public void breakpointAdded(IBreakpoint breakpoint) {
        if(!(breakpoint instanceof JavaBreakpoint)) return;
        JavaBreakpoint jbp = (JavaBreakpoint)breakpoint;
        try{
            jbp.addToTarget(this);
        }catch(DebugException ex){
            logger.error("Could not install breakpoint.", ex);
            return;
        }
        installedBreakpoints.add(jbp);
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        if(!(breakpoint instanceof JavaBreakpoint)) return;
        JavaBreakpoint jbp = (JavaBreakpoint)breakpoint;
        try{
            jbp.cancelForTarget(this);
        }catch(DebugException ex){
            logger.error("Failed to remove breakpoint.", ex);
            return;
        }
        
        installedBreakpoints.remove(jbp);
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) { }

    public boolean canDisconnect() {
        return !isDisconnected();
    }

    public void disconnect() throws DebugException {
        if(!supportsDisconnect()){
            this.notSupported("Disconnection not supported.");
        }else if(isDisconnected()){
            return;
        }
        vmm.disconnect();
    }

    public boolean supportsStorageRetrieval() {
        return false;
    }

    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
        this.requestFailed("Java nodes don't support storage retrieval.", null);
        return null;
    }
    
    protected boolean supportsTerminate(){
        return supportsTerminate;
    }
    
    protected boolean supportsDisconnect(){
        return supportsDisconnect;
    }

    public boolean isDisconnected(){
        return !vmm.isConnected();
    }

    public boolean isTerminated() {
        return vmm.isTerminated();
    }
    
    public boolean isSuspended(){
        return vmm.isSuspended();
    }

    public VirtualMachineManager getVMManager() {
        return vmm;
    }

    public void handleDeath() {
        fireTerminateEvent();
    }

    public void handleDisconnect() { }

    public void handleSuspend(int detail) {
        fireSuspendEvent(detail);
    }
    
    public void handleResume(int detail){
        fireResumeEvent(detail);
    }
    
    
}
