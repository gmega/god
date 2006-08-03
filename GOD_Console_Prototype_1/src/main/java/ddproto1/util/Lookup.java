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

    private static final IServiceRegistry instance = new Lookup();
    
    private volatile boolean running = false;

    public static IServiceRegistry serviceRegistry() {
        return instance;
    }

    private Map<String, Object> registry = new HashMap<String, Object>();

    private Lookup() { }
    
    public synchronized void start(){ running = true; }
    
    public synchronized void stop(){
        checkRunning();
        registry.clear();
        running = false;
    }
    
    private synchronized boolean startState(){
        return running;
    }

    public Object locate(String serviceName) throws NoSuchSymbolException {
        Object service;
        synchronized(this){
            checkRunning();
            service = registry.get(serviceName);
        }
        
        if (service == null)
            throw new NoSuchSymbolException("Could not locate service "
                    + serviceName);
        return service;
    }

    public synchronized void register(String name, Object service)
            throws DuplicateSymbolException {
        checkRunning();
        if (registry.containsKey(name))
            throw new DuplicateSymbolException(
                    "There's already a service bound under the name of " + name);

        registry.put(name, service);
    }

    public synchronized void unregister(String name) throws NoSuchSymbolException {
        checkRunning();
        if (!registry.containsKey(name))
            throw new NoSuchSymbolException("Could not locate service " + name);

        registry.remove(name);
    }
    
    private void checkRunning(){
        if(!running) throw new IllegalStateException("Service registry is not running");
    }

    public int currentState() {
        return startState()?STARTED:STOPPED;
    }
}
