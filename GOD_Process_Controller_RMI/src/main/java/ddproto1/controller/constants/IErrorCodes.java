/*
 * Created on Jun 17, 2006
 * 
 * file: IErrorCodes.java
 */
package ddproto1.controller.constants;

/**
 * Error codes for server-side operations.
 * 
 * @author giuliano
 *
 */
public interface IErrorCodes {
    /**
     * Indicates no failure.
     */
    public static final int STATUS_OK = 0;
    
    /**
     * Indicates a failed request.
     */
    public static final int STATUS_ERROR = 1;
    
    /**
     * Bad parameter detail. Means that the request failed 
     * because one or more parameters (passed to a method) 
     * are invalid.
     */
    public static final int INVALID_PARAMETER = -1;
    
    /**
     * I/O failure detail. Means that the request failed 
     * due to an I/O error at the server side.
     */
    public static final int IO_FAILURE = -2;
    
    /**
     * Timeout failure detail. Means that the request failed
     * because of a timeout at the server side.
     */
    public static final int TIMEOUT = -3;
    
    /**
     * Unspecified failure detail. The source of the failure
     * didn't specify why it failed.
     */
    public static final int UNSPECIFIED = -4;

}
