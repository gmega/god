/*
 * Created on Apr 12, 2005
 * 
 * file: IncarnationException.java
 */
package ddproto1.exception;

public class IncarnationException extends Exception {

    private static final long serialVersionUID = 3258407331191337011L;

    public IncarnationException() {
        super();
    }

    public IncarnationException(String message) {
        super(message);
    }

    public IncarnationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncarnationException(Throwable cause) {
        super(cause);
    }

}
