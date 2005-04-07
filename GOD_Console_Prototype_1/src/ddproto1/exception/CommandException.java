/*
 * Created on Nov 23, 2004
 * 
 * file: CommandException.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class CommandException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3258410612530100530L;

    /**
     * 
     */
    public CommandException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public CommandException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public CommandException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
