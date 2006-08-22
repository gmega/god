/*
 * Created on Oct 24, 2005
 * 
 * file: LaunchCentralAgent.java
 */
package ddproto1.plugin.ui.launching;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
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

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.plugin.ui.DDUIPlugin;

public class LaunchCentralAgentDelegate implements IWorkbenchWindowActionDelegate, IConfigurationConstants{

    private static final Logger logger = DDUIPlugin.getDefault().getLogManager().getLogger(LaunchCentralAgentDelegate.class);
    
    public void run(IAction action) {
        try{
            
            /** 
             * We pass the root node configuration attributes to the configuration copy.
             */
            DebugPlugin plugin = DebugPlugin.getDefault();
            ILaunchConfigurationType gacType = 
                plugin.getLaunchManager().getLaunchConfigurationType(IConfigurationConstants.ID_GLOBAL_AGENT_APPLICATION);
            ILaunchConfigurationWorkingCopy workingCopy = gacType.newInstance(null, CENTRAL_AGENT_CONFIG_NAME);
            
            IObjectSpec rootSpec = DDUIPlugin.getDefault().getConfigurationManager().getRootSpec();
            
            try{
                /** Please note that the attributes are hardwired, there's no translation level. Exercise 
                 * care if you change the xml specs. */
                workingCopy.setAttribute(THREAD_POOL_SIZE, rootSpec.getAttribute(THREAD_POOL_SIZE));
                workingCopy.setAttribute(MAX_QUEUE_LENGTH, rootSpec.getAttribute(MAX_QUEUE_LENGTH));
                workingCopy.setAttribute(CDWP_PORT, rootSpec.getAttribute(CDWP_PORT));
            }catch(AttributeAccessException ex){
                logger.error("Failed to launch central agent due to a configuration error.", ex);
                return;
            }
            
            ILaunchConfiguration config = workingCopy.doSave();
            DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
            
        }catch(CoreException ex){
            logger.error("Error while launching the central agent.", ex);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) { }
    public void dispose() { }
    public void init(IWorkbenchWindow window) { }

}
