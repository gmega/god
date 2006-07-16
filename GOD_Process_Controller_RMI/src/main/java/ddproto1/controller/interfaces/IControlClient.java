/*
 * Created on Jun 21, 2006
 * 
 * file: ControlClient.java
 */
package ddproto1.controller.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IControlClient extends Remote{
    /**
     * Notifies the client that the process of handle pHandle
     * has died.
     */
    void notifyProcessDeath(int pHandle, int exitValue)
        throws RemoteException;
    
    /**
     * Notifies the client that a new segment of characters has
     * been sent to the standard output of the remote application.
     */
    void receiveStringFromSTDOUT(int pHandle, String data)
        throws RemoteException;
    
    /**
     * Notifies the client that a new segment of characters has
     * been sent to the standard error output of the remote 
     * application.
     */
    void receiveStringFromSTDERR(int pHandle, String data)
        throws RemoteException;
    
    /**
     * Notifies the client that this process server is up 
     * and running. Useful for synchronization if the client 
     * is the one launching the server.
     */
    void notifyServerUp(IProcessServer procServer)
        throws RemoteException;

}
