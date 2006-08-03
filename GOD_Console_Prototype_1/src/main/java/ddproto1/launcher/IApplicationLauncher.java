/*
 * Created on Jul 13, 2006
 *
 */
package ddproto1.launcher;

import java.util.List;

import org.eclipse.debug.core.ILaunch;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.LauncherException;
import ddproto1.launcher.procserver.IProcessEventListener;


/**
 * This interface represents an application launcher.
 * 
 * @author giuliano
 *
 */
public interface IApplicationLauncher extends IConfigurable{
    
    public static final String PROC_HANDLE_ATTRIBUTE = "process-handle";
 
    /**
     * Launches an application.
     * 
     * @param launch ILaunch instance that will be returned in the getProcess method
     * of the returned process.
     * 
     * @param listeners List containing listeners that should be registered before the
     * process is launched. These listeners are guaranteed to receive ALL events. 
     *  
     * @return A reference to the launched process.
     * 
     * @throws ConfigException if there are unsolved issues with the configuration.
     * @throws IncarnationException if some dependency fails to incarnate.
     * @throws LauncherException if the application cannot be launched.
     */
    public RemoteGODProcess launchOn(ILaunch launch, List<IProcessEventListener> listeners)
    	throws ConfigException, IncarnationException, LauncherException;
}
