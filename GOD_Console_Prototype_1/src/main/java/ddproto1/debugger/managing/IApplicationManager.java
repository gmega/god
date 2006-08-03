/*
 * Created on Jul 16, 2006
 * 
 * file: IApplicationManager.java
 */
package ddproto1.debugger.managing;

import org.eclipse.core.runtime.CoreException;

import ddproto1.exception.ConfigException;

public interface IApplicationManager {
    public GlobalAgent launchGlobalAgent() throws ConfigException, CoreException;
    
}
