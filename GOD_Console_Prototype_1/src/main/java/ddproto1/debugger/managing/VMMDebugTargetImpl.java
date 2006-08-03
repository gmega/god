/*
 * Created on Nov 22, 2005
 * 
 * file: VMMDebugTargetImpl.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageTranscoder;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaLineBreakpoint;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.VMStartEvent;

import ddproto1.GODBasePlugin;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.request.DeferrableClassPrepareRequest;
import ddproto1.debugger.request.DeferrableHookRequest;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.JDIEventProcessorTrait;
import ddproto1.util.traits.JDIEventProcessorTrait.JDIEventProcessorTraitImplementor;

public class VMMDebugTargetImpl extends JavaDebugElement implements IJavaDebugTarget, JDIEventProcessorTraitImplementor{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(VMMDebugTargetImpl.class);

    private ILaunch launch;
    
    private IJavaNodeManager vmm;
    
    private boolean supportsTerminate;
    
    private boolean supportsDisconnect;
    
    private boolean resumeOnStart;
    
    private IProcess process;
    
    private List<JavaBreakpoint> installedBreakpoints = new ArrayList<JavaBreakpoint>();
    private BidiMap mappedBreakpoints = new DualHashBidiMap();
    
    public VMMDebugTargetImpl(ILaunch launch, boolean supportTerminate, 
            boolean supportDisconnect, IProcess process, boolean resume, 
            IJavaNodeManager nManager)
        throws DebugException
    {
        super(null);
        this.setProcess(process);
        setSupportsTerminate(supportTerminate);
        setSupportsDisconnect(supportDisconnect);
        setResumeOnStartup(resume);
        setProcess(process);
        setLaunch(launch);
        
        try{
        	vmm = nManager;
            initializeBreakpoints();
            
            // One day I won't need the following line
            // (which is a part of a disgusting hack):
            DeferrableClassPrepareRequest.registerVM(vmm.getName());
            vmm.getDeferrableRequestQueue().addEagerlyResolve(
            		new DeferrableHookRequest(vmm.getName(),
							IEventManager.VM_START_EVENT,
							new JDIEventProcessorTrait(this), null));
            
        }catch(Exception ex){
            throw new DebugException(new Status(IStatus.ERROR,
                    GODBasePlugin.getDefault().getBundle().getSymbolicName(), 
                    IStatus.ERROR,
                    "Failed to create a proxy for the virtual machine:" + ex.getMessage(), ex));
        }
    }
    
    protected void setLaunch(ILaunch launch){
        this.launch = launch;
    }
    
    @Override
    public IDebugTarget getDebugTarget(){
        return this;
    }
    
    @Override
    public ILaunch getLaunch(){
    		return launch;
    }
    
    public void bindTo(VirtualMachineManager vmm){
        if(this.getVMM() != null)
            throw new IllegalStateException("Cannot bind " +
                    "target to more than one Virtual Machine Manager.");
        this.setVMM(vmm);
    }
    
    protected void setSupportsTerminate(boolean supportsTerminate){
        this.supportsTerminate = supportsTerminate;
    }

    protected void setVMM (VirtualMachineManager vmm) { this.vmm = vmm; }
    
    /**
     * @return Returns the vmm.
     */
    protected IJavaNodeManager getVMM() {
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
        try{
            if(vmm.isAvailable())
    		        return vmm.getThreadManager().getThreads();
    	
            return new IThread[0];
        }catch(Exception ex){
            logger.error("Error retrieving thread list", ex);
            requestFailed("Could not retrieve thread list.", ex);
            return null; // Never happens.
        }
    }

    public boolean hasThreads() throws DebugException {
		try {
			return vmm.getThreadManager().hasThreads();
		} catch (VMDisconnectedException ex) {
			return false;
		}
	}

    public String getName() throws DebugException {
        return vmm.getName();
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return (breakpoint instanceof JavaLineBreakpoint);
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
        IBreakpoint[] bps = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
        if(bps == null) return;
        for (int i = 0; i < bps.length; i++) {
            if (bps[i] instanceof JavaLineBreakpoint || bps[i] instanceof JavaBreakpoint) {
                breakpointAdded(bps[i]);
            }
        }
    }
    
    public void resume() throws DebugException {
        
    }

    public void suspend() throws DebugException {
        
    }

    public void breakpointAdded(IBreakpoint breakpoint) {
		if (!supportsBreakpoint(breakpoint))
			return;

		try {
			JavaBreakpoint jb;

			if (breakpoint instanceof JavaLineBreakpoint){
				jb = mapJavaLineBreakpoint((JavaLineBreakpoint) breakpoint);
			}else if(breakpoint instanceof JavaBreakpoint) {
				jb = (JavaBreakpoint)breakpoint;
			}else {
				logger.warn("Cannot add breakpoint of type "
						+ breakpoint.getClass().getName());
				return;
			}

			jb.addToTarget(this);
			installedBreakpoints.add(jb);
		} catch (Exception ex) {
			logger.error("Could not install breakpoint.", ex);
			return;
		}
	}
    
    protected JavaBreakpoint mapJavaLineBreakpoint(JavaLineBreakpoint bp)
    		throws CoreException
    {
    		/** Maps a JDT JavaLineBreakpoint into our implementation. */
    		int lineNumber = bp.getLineNumber();
    		String typeName = bp.getTypeName();
    		assert typeName != null;
    		
    		JavaBreakpoint jb = new JavaBreakpoint(typeName, lineNumber, bp);
    		jb.setMarker(bp.getMarker());
    		
    		mappedBreakpoints.put(bp, jb);
    		
    		return jb;
    }
    
    protected JavaBreakpoint javaBreakpointFrom(IBreakpoint bkp){
    		return (JavaBreakpoint)mappedBreakpoints.get(bkp);
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    		JavaBreakpoint jbp = javaBreakpointFrom(breakpoint);
    		if(jbp == null) return;
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

    public IJavaNodeManager getVMManager() {
        return vmm;
    }

    public void handleDeath() {
        IBreakpointManager bm = DebugPlugin.getDefault().getBreakpointManager();
        bm.removeBreakpointListener(this);
        try{
        		this.removeAllBreakpoints();
        }catch(DebugException ex){
        		logger.error("Failed to remove breakpoint from target.", ex);
        }
        fireTerminateEvent();
    }
    
    private void removeAllBreakpoints()
    		throws DebugException
    {
        /** Sweep through installed breakpoints, remove them
         * and unmap them.
         */
        
        for(JavaBreakpoint installed : getInstalledBreakpoints()){
            installed.cancelForTarget(this);
            unmapJavaBreakpoint(installed);
        }
        
        getInstalledBreakpoints().clear();
    }
    
    private void unmapJavaBreakpoint(JavaBreakpoint installed){
        if(!mappedBreakpoints.containsValue(installed)) 
            throw new RuntimeException("Attempt to unmap non-mapped breakpoint.");
        mappedBreakpoints.removeValue(installed);
    }
    
    protected List<JavaBreakpoint> getInstalledBreakpoints(){
        return installedBreakpoints;
    }
    
    public Object getAdapter(Class klass){
        Object adapter = 
            super.getAdapter(klass);
        
        if(adapter == null){
            if(klass.isAssignableFrom(this.getClass())) adapter = this;
            if(klass.isAssignableFrom(VirtualMachineManager.class)) adapter = vmm;
        }
        
        return adapter;
    }

    public void handleDisconnect() { }

    public void handleSuspend(int detail) {
        fireSuspendEvent(detail);
    }
    
    public void handleResume(int detail){
        fireResumeEvent(detail);
    }

	public void specializedProcess(Event request) {
		if(!(request instanceof VMStartEvent)){
			throw new UnsupportedException("VMStartEvents are the only " +
					"type supported by this handler.");
		}
		this.fireCreationEvent();
		launch.addDebugTarget(this);
	}

	/** For the event processor. */
	private boolean enabled = true;
	private IJDIEventProcessor next;
		
	public void next(IJDIEventProcessor next) { this.next = next; }
	public IJDIEventProcessor next()          { return next; }
	public boolean enabled()                  { return enabled; }
	public void enabled(boolean newValue)     { this.enabled = newValue; }
}
