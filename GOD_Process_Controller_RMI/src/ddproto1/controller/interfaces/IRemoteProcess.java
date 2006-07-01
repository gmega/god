/*
 * Created on Jun 21, 2006
 * 
 * file: IRemoteProcess.java
 */
package ddproto1.controller.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ddproto1.controller.exception.ServerRequestException;

/**
 * 
 * Not meant to be serializable (access should be through proxies).
 * 
 * @author giuliano
 *
 */
public interface IRemoteProcess extends Remote{
    /**
     * Returns true if the remote process has terminated, and
     * false otherwise.
     */
    boolean isAlive() throws RemoteException;
    /**
     * Writes a sequence of characters to the standard input
     * of the remote process. Flush is immediate.
     *
     * @throws ServerRequestException if an I/O exception occurs
     * at the server side.
     */
    void writeToSTDIN(String message) throws ServerRequestException, RemoteException;
    
    /**
     * Returns the numeric handle of this remote process. Uniqueness
     * of numeric handles is implementation-dependent. The default 
     * implementation, for instance, delegates the task of ensuring
     * handle uniqueness to the client.
     */
    int getHandle() throws RemoteException;
    
    /**
     * Disposes of this proxy. This will cause the remote 
     * process to be (forcefully) terminated. Subsequent calls to 
     * ProcessServer::getProcessList() will not return this
     * proxy as well. All calls dispatched to this proxy will
     * likely result in exceptions after dispose is called. 
     */
    void dispose() throws RemoteException;
}
