/*
 * Created on Jul 22, 2004
 *
 */
package ddproto1.launcher;

import java.util.List;

import ddproto1.exception.CommException;
import ddproto1.exception.InvalidStateException;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IShellTunnel extends ITunnel {
    public void feedCommand(String s) throws CommException, InvalidStateException;
    public List<String> getStdoutResult() throws InvalidStateException;
    public List<String> getStderrResult() throws InvalidStateException;
}
