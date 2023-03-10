/*
 * Created on Oct 10, 2005
 * 
 * file: DTNodeLaunchConfigurationDelegate.java
 */
package ddproto1.launcher.delegates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.LaunchHelper;
import ddproto1.debugger.managing.GlobalAgent;
import ddproto1.debugger.managing.ILocalNodeManager;

public class DTNodeLaunchConfigurationDelegate implements ILaunchConfigurationDelegate{
    
	private LaunchHelper am = new LaunchHelper();
	
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        
        /** Checks to see if the global agent is running. */
        IDebugTarget gAgentTarget = findGlobalAgentTarget();
        if(gAgentTarget == null || gAgentTarget.isTerminated() || gAgentTarget.isDisconnected())
            launchFailed("Global agent must be started before launching remote processes.", null);
        
        GlobalAgent gAgent = (GlobalAgent) gAgentTarget.getAdapter(GlobalAgent.class);
        
        String nodeName = configuration.getAttribute(IConfigurationConstants.NAME_ATTRIBUTE, "");
        if(nodeName.equals(""))
            launchFailed("Node name is absent from configuration.", null);
        
        IObjectSpec nodeSpec = GODBasePlugin.getDefault()
                .getConfigurationManager().getNodelist().getSpec(nodeName);
        if(nodeSpec == null)
            launchFailed("Failed to acquire in-memory configuration specification.", null);
        
        try {
            ILocalNodeManager inm = am.launchApplication(nodeSpec, launch);
            ILocalNodeManager jnm = (ILocalNodeManager) inm
                    .getAdapter(ILocalNodeManager.class);
            jnm.connect();
            gAgent.addTarget((IDebugTarget)jnm.getAdapter(IDebugTarget.class));

        } catch (Exception ex) {
            launchFailed("Launch failed.", ex);
        }
    }
    
    private IDebugTarget findGlobalAgentTarget()
        throws DebugException
    {
        IDebugTarget [] allTargets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
        for(IDebugTarget target : allTargets)
            if(target.getName().equals(IConfigurationConstants.CENTRAL_AGENT_NAME))
                return target;
        return null;
    }
    
    private void launchFailed(String reason, Exception nested) throws CoreException{
        throw new CoreException(new Status(IStatus.ERROR,
                IConfigurationConstants.ID, IStatus.ERROR, reason, nested));
    }
}
