/*
 * Created on Jun 17, 2006
 * 
 * file: IErrorCodes.java
 */
package ddproto1.controller.remote.impl;

/**
 * Error codes for server-side operations.
 * 
 * @author giuliano
 *
 */
public interface IErrorCodes {
    /**
     * Bad parameter. Means that one or more parameters
     * (passed to a method) are invalid.
     */
    public static final int INVALID_PARAMETER = -1;
    
    /**
     * Request couldn't be completed due to an I/O error
     * at the server side.
     */
    public static final int IO_FAILURE = -2;
}
