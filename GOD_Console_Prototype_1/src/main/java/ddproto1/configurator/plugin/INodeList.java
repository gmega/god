/*
 * Created on Oct 23, 2005
 * 
 * file: INodeList.java
 */
package ddproto1.configurator.plugin;

import ddproto1.configurator.IObjectSpec;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;

public interface INodeList {
    public void        rebindSpec  (IObjectSpec spec) throws DuplicateSymbolException, AttributeAccessException;
    public void        unbindSpec  (IObjectSpec spec) throws NoSuchSymbolException, AttributeAccessException;
    public IObjectSpec getSpec     (String name);
}
