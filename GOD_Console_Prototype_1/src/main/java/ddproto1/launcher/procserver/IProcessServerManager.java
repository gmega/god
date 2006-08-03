/*
 * Created on 18/07/2006
 * 
 * file: IProcessServerManager.java
 */
package ddproto1.launcher.procserver;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.exception.LauncherException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.util.IServiceLifecycle;

public interface IProcessServerManager extends IServiceLifecycle {

    public IProcessServer getProcessServerFor(IConfigurable configurable,
            IRemoteCommandExecutor executor) throws AttributeAccessException,
            UnknownHostException, RemoteException, InterruptedException,
            ExecutionException, LauncherException;

    /**
     * 
     * @param listener
     * @param pHandle
     */
    public void registerProcessListener(IProcessEventListener listener,
            int pHandle);

    /**
     * 
     * @param listener
     * @param pHandle
     * @return
     */
    public boolean removeProcessListener(IProcessEventListener listener,
            int pHandle);

    public boolean startIfPossible() throws Exception;
}