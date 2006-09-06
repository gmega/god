/*
 * Created on Oct 24, 2005
 * 
 * file: StopCentralAgentDelegate.java
 */
package ddproto1.plugin.ui.launching;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.util.MessageHandler;

public class StopCentralAgentDelegate implements IWorkbenchWindowActionDelegate {

    private static final Logger logger = MessageHandler.getInstance().getLogger(StopCentralAgentDelegate.class);
    
    public void run(IAction action) {
        /** Finds the central agent debug target */
        IDebugTarget [] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
        IDebugTarget caTarget= null;
        for(IDebugTarget target : targets){
            try{
                if(target.getLaunch().getLaunchConfiguration().getName().equals(IConfigurationConstants.CENTRAL_AGENT_CONFIG_NAME) &&
                        target.getName().equals(IConfigurationConstants.CENTRAL_AGENT_NAME)){
                    caTarget= target;
                }
            }catch(DebugException ex) {}
        }
        
        if(caTarget != null){
            try{
                caTarget.terminate();
            }catch(DebugException ex){
                logger.error("Error while attempting to stop the central agent.", ex);
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) { }
    public void dispose() { }
    public void init(IWorkbenchWindow window) { }

}
