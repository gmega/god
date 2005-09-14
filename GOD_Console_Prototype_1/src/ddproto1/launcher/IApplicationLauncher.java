/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.launcher;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.ConfigException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.commons.CommException;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IApplicationLauncher extends IConfigurable{
    
    public void launch()
    	throws CommException, LauncherException, ConfigException;

}
