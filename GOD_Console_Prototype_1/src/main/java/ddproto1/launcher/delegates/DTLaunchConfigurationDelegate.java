/*
 * Created on Oct 20, 2005
 * 
 * file: DTLaunchConfigurationDelegate.java
 */
package ddproto1.launcher.delegates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import ddproto1.GODBasePlugin;
import ddproto1.debugger.managing.GlobalAgent;

public class DTLaunchConfigurationDelegate implements ILaunchConfigurationDelegate{

    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        GlobalAgent ga = 
            GODBasePlugin.getDefault().getGlobalAgent();
        if(ga.isRunning()) return;
        ga.start(monitor);
        launch.addDebugTarget(ga);
    }
    

}
