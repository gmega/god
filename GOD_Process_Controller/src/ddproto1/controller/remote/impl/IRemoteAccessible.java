/*
 * Created on Jun 20, 2006
 * 
 * file: IRemoteAccessible.java
 */
package ddproto1.controller.remote.impl;

import Ice.ObjectPrx;

/**
 * Interface that represents a remotely accessible object.
 * 
 * @author giuliano
 *
 */
public interface IRemoteAccessible {
    /**
     * Returns a remote proxy for this object.
     * 
     * @return 
     */
    public ObjectPrx activateAndGetProxy();
    
    /**
     * Tells this object that it's no longer needed.
     *
     */
    public void dispose();
}
