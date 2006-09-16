/*
 * Created on 8/09/2006
 * 
 * file: JDTSourcePathComputerWrapper.java
 */
package ddproto1.sourcemapper.generic;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;

public class DelegatingSourcePathComputer implements ISourcePathComputerDelegate{

    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) 
        throws CoreException {
        
        String associatedName = configuration.getAttribute(IConfigurationConstants.ASSOCIATED_CONFIG_NAME, "");
        if(associatedName.equals("")) GODBasePlugin.throwCoreExceptionWithError("Cannot initialize source mapping - " +
                "no associated configuration defined.", null);
        
        ILaunchConfiguration associated = null;
        
        for(ILaunchConfiguration config : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()){
            if(config.getName().equals(associatedName)){
                associated = config;
                break;
            }
        }
        
        if(associated == null)
            GODBasePlugin.throwCoreExceptionWithError("Could not locate associated configuration " + associatedName + 
                    ". Source mapping may not work properly.", null);
        
        ISourcePathComputer spc = associated.getType().getSourcePathComputer();
        
        return spc.computeSourceContainers(associated, monitor);
    }
}
