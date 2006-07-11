/*
 * Created on Jun 21, 2006
 * 
 * file: IRemotable.java
 */
package ddproto1.controller.interfaces;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemotable {
    public Remote getProxyAndActivate() throws RemoteException, NoSuchObjectException;
}
