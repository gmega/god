/*
 * Created on Sep 27, 2004
 * 
 * file: RequestProcessorException.java
 */
package ddproto1.exception;


/**
 * @author giuliano
 *
 */
public class RequestProcessorException extends RuntimeException{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RequestProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestProcessorException(String message) {
        super(message);
    }

    public RequestProcessorException(Throwable cause) {
        super(cause);
    }

}
