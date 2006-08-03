/*
 * Created on Jul 22, 2004
 *
 */
package ddproto1.launcher;

import java.util.List;

import org.eclipse.debug.core.IStreamListener;

import ddproto1.exception.InvalidStateException;
import ddproto1.exception.commons.CommException;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IShellTunnel extends ITunnel, IStreamManager {
    public void feedCommand     (String s) throws CommException, InvalidStateException;
}
