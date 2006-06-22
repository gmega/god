/*
 * Created on Jun 21, 2006
 * 
 * file: ProcessServer.java
 */
package ddproto1.controller.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ddproto1.controller.exception.ServerRequestException;

public interface IProcessServer extends Remote{
    /** 
     * Launches a remote process with the given parameters.
     *
     * @throws ServerRequestException if launch fails.
     */
    IRemoteProcess launch(LaunchParametersDTO parameters)
        throws ServerRequestException, RemoteException;
    
   /** 
    * Retrieves a list of registered processes, dead or not. A RemoteProcess instance
    * gets out of this list only after RemoteProcess.dispose() is called. 
    */
    List<IRemoteProcess> getProcessList()
        throws RemoteException;
    
    /**
     * Keep alive method for the process server.
     */
    boolean isAlive()
        throws RemoteException;
    
    /**
     * Shuts down the remote process server. If 'true' is passed on as a
     * parameter, all controlled processes will be killed as well. Otherwise
     * they'll be left running.
     */
    void shutdownServer(boolean shutdownChildProcesses)
        throws RemoteException;
    
}
