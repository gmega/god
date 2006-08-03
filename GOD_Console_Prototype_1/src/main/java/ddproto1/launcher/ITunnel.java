/*
 * Created on Jul 22, 2004
 *
 */
package ddproto1.launcher;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.CommException;

/**
 * @author giuliano
 *
 */
public interface ITunnel extends IConfigurable{
    public void open() throws CommException, ConfigException;
    public void close(boolean force) throws CommException, ConfigException;
}
