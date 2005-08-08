/*
 * Created on Aug 8, 2005
 * 
 * file: Lookup.java
 */
package ddproto1.util;

import java.util.HashMap;
import java.util.Map;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;

public class Lookup implements IServiceRegistry {

    private static IServiceRegistry instance;

    public static IServiceRegistry serviceRegistry() {
        return (instance == null) ? (new Lookup()) : instance;
    }

    private Map<String, Object> registry = new HashMap<String, Object>();

    private Lookup() { }

    public Object locate(String serviceName) throws NoSuchSymbolException {
        Object service = registry.get(serviceName);
        if (service == null)
            throw new NoSuchSymbolException("Could not locate service "
                    + serviceName);
        return service;
    }

    public void register(String name, Object service)
            throws DuplicateSymbolException {
        if (registry.containsKey(name))
            throw new DuplicateSymbolException(
                    "There's already a service bound under the name of " + name);

        registry.put(name, service);
    }

    public void unregister(String name) throws NoSuchSymbolException {
        if (!registry.containsKey(name))
            throw new NoSuchSymbolException("Could not locate service " + name);

        registry.remove(name);
    }
}
