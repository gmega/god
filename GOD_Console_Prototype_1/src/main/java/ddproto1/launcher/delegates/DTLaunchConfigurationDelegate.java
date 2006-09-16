/*
 * Created on Oct 20, 2005
 * 
 * file: DTLaunchConfigurationDelegate.java
 */
package ddproto1.launcher.delegates;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;

import ddproto1.GODBasePlugin;
import ddproto1.debugger.managing.GlobalAgent;
import ddproto1.debugger.managing.GlobalAgentProcess;
import ddproto1.launcher.procserver.IProcessServerManager;
import ddproto1.sourcemapper.generic.DelegatingSourceLocator;

public class DTLaunchConfigurationDelegate implements ILaunchConfigurationDelegate{

    public void launch(ILaunchConfiguration configuration, String mode, 
            ILaunch launch, IProgressMonitor monitor) throws CoreException {
        GODBasePlugin gbPlugin = GODBasePlugin.getDefault();
        GlobalAgent ga = gbPlugin.getGlobalAgent();
        if(ga.isRunning()) return;

        Map attributes = configuration.getAttributes();
        Preferences prefs = gbPlugin.getPluginPreferences();
        
        for(Object attKey : attributes.keySet()){
            String value = (String)attributes.get(attKey);
            prefs.setValue((String)attKey, value);
        }
        
        IProcessServerManager psManager = gbPlugin.getProcessServerManager();
        try{
            psManager.start();
        }catch(Exception ex){
            GODBasePlugin.throwCoreExceptionWithError(
                    "Error while attempting to start process server manager.",
                    ex);
        }
        
        IProcess gaProcess = new GlobalAgentProcess(launch, "Global Agent", psManager);
        launch.addProcess(gaProcess);
        launch.addDebugTarget(ga);
        launch.setSourceLocator(new DelegatingSourceLocator());
        ga.start(monitor, launch, gaProcess);
    }
    

}
