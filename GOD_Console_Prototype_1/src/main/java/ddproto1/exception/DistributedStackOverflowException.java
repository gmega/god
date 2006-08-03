/*
 * Created on Sep 22, 2005
 * 
 * file: DistributedStackOverflowException.java
 */
package ddproto1.exception;

public class DistributedStackOverflowException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DistributedStackOverflowException() {
        super();
    }

    public DistributedStackOverflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistributedStackOverflowException(String message) {
        super(message);
    }

    public DistributedStackOverflowException(Throwable cause) {
        super(cause);
    }
}
