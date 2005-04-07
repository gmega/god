/*
 * Created on Feb 3, 2005
 * 
 * file: MalformedInputException.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class MalformedInputException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 3761686771427455289L;

    /**
     * 
     */
    public MalformedInputException() {
        super();
    }

    /**
     * @param message
     */
    public MalformedInputException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public MalformedInputException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public MalformedInputException(Throwable cause) {
        super(cause);
    }

}
