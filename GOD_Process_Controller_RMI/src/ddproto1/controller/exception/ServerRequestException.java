/*
 * Created on Jun 21, 2006
 * 
 * file: ServerRequestException.java
 */
package ddproto1.controller.exception;

import java.rmi.RemoteException;

public class ServerRequestException extends RemoteException {

    /**
     * 
     */
    private static final long serialVersionUID = 5760342086640630411L;

    public ServerRequestException() { super(); }
    public ServerRequestException(String s, Throwable cause) { super(s, cause); }
    public ServerRequestException(String s) { super(s); }

}
