/*
 * Created on Jun 21, 2006
 * 
 * file: ServerRequestException.java
 */
package ddproto1.controller.exception;

import java.rmi.RemoteException;

import ddproto1.controller.constants.IErrorCodes;

public class ServerRequestException extends RemoteException implements IErrorCodes{

    private static final long serialVersionUID = 5760342086640630411L;

    private final int fDetail;

    public ServerRequestException(String message, Throwable cause, int detail){
        super(message, cause);
        fDetail = detail;
    }
    
    public ServerRequestException() { this(null, null, UNSPECIFIED); }
    public ServerRequestException(String s, Throwable cause) { this(s, cause, UNSPECIFIED); }
    public ServerRequestException(String s) { this(s, null, UNSPECIFIED); }
    
    public int getDetail() { return fDetail; }

}
