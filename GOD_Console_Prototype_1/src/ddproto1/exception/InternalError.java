/*
 * Created on Nov 25, 2004
 * 
 * file: InternalError.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class InternalError extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 3617009749971908919L;

    /**
     * 
     */
    public InternalError() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public InternalError(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public InternalError(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public InternalError(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
