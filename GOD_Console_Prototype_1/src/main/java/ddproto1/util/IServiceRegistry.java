/*
 * Created on Aug 8, 2005
 * 
 * file: IServiceRegistry.java
 */
package ddproto1.util;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;

public interface IServiceRegistry extends IServiceLifecycle{
    public Object locate(String serviceName) throws NoSuchSymbolException;
    public void register(String name, Object service) throws DuplicateSymbolException;
    public void unregister(String name) throws NoSuchSymbolException;
}
