/*
 * Created on Jun 26, 2006
 * 
 * file: IRemoteCommandExecutor.java
 */
package ddproto1.launcher.procserver;

import java.io.IOException;

import ddproto1.exception.ConfigException;
import ddproto1.launcher.ICommandLine;

/**
 * Interface intended to insulate the ProcessServerManager from the underlying
 * implementation that is used to launch the remote process server.
 * 
 * @author giuliano
 *
 */
public interface IRemoteCommandExecutor {
    public Process executeRemote()
    		throws IOException, ConfigException;
    public ICommandLine getCommandLine()
            throws ConfigException;
    public void setCommandLine(ICommandLine line);
}
