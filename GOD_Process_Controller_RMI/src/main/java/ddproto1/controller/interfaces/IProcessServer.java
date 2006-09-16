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
    public IRemoteProcess launch(LaunchParametersDTO parameters)
        throws ServerRequestException, RemoteException;
    
   /** 
    * Retrieves a list of registered processes, dead or not. A RemoteProcess instance
    * gets out of this list only after RemoteProcess.dispose() is called. 
    */
    public List<IRemoteProcess> getProcessList()
        throws RemoteException;
    
    /**
     * Keep alive method for the process server.
     */
    public boolean isAlive()
        throws RemoteException;
    
    /**
     * Shuts down the remote process server with the specified
     * timeout. A timeout of zero means "wait forever". A negative
     * timeout means that the server should be shut down 
     * asynchronously (method returns before server has sucessfuly
     * shut down).
     * 
     * @throws ServerRequestException if the specified timeout 
     * has ellapsed and the process server wasn't able to shutdown
     * all children. The exception thrown will have a detail of 
     * IErrorCodes.TIMEOUT. The process server won't shutdown in
     * this case.
     * 
     * @throws RemoteException if some other failure occurs.
     * 
     * @see ddproto1.controller.constants.IErrorCodes
     */
    public void shutdownServer(boolean shutdownChildren, long childrenTimeout)
        throws ServerRequestException, RemoteException;
    
    /**
     * Returns a string cookie that might have been set for this process
     * server. 
     * 
     * @return the string cookie, or an empty string if there's no cookie.
     */
    public String getCookie()
    		throws RemoteException;
    
}
