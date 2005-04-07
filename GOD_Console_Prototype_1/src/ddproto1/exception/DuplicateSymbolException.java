/*
 * Created on Feb 2, 2005
 * 
 * file: DuplicateSymbolException.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class DuplicateSymbolException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3978988756277998131L;

    /**
     * 
     */
    public DuplicateSymbolException() {
        super();
    }

    /**
     * @param message
     */
    public DuplicateSymbolException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public DuplicateSymbolException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public DuplicateSymbolException(Throwable cause) {
        super(cause);
    }

}
