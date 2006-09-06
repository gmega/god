/*
 * Created on Jun 5, 2006
 * 
 * file: INodeManagerFactory.java
 */
package ddproto1.debugger.managing;

import ddproto1.configurator.IObjectSpec;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.commons.AttributeAccessException;

public interface INodeManagerFactory {
    
    public ILocalNodeManager createNodeManager(IObjectSpec vmmspec)
        throws ConfigException, AttributeAccessException,
        IncarnationException;
}
