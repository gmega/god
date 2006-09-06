/*
 * Created on Oct 24, 2005
 * 
 * file: LaunchCentralAgent.java
 */
package ddproto1.plugin.ui.launching;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.plugin.ui.DDUIPlugin;

public class LaunchCentralAgentDelegate implements IWorkbenchWindowActionDelegate, IConfigurationConstants{

    private static final Logger logger = DDUIPlugin.getDefault().getLogManager().getLogger(LaunchCentralAgentDelegate.class);
    
    public void run(IAction action) {
        try{
            
            DebugPlugin plugin = DebugPlugin.getDefault();
            ILaunchConfigurationType gacType = 
                plugin.getLaunchManager().getLaunchConfigurationType(IConfigurationConstants.ID_GLOBAL_AGENT_APPLICATION);
            ILaunchConfigurationWorkingCopy workingCopy = gacType.newInstance(null, CENTRAL_AGENT_CONFIG_NAME);

            /** Create launch configuration from preferences */
            Preferences prefs = DDUIPlugin.getDefault().getPluginPreferences(); 
            String [] setPrefs = prefs.propertyNames();
            
            for(String setPref : setPrefs){
                workingCopy.setAttribute(setPref, prefs.getString(setPref));
            }
            
            ILaunchConfiguration config = workingCopy.doSave();
            config.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
        }catch(CoreException ex){
            logger.error("Error while launching the central agent.", ex);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) { }
    public void dispose() { }
    public void init(IWorkbenchWindow window) { }

}
