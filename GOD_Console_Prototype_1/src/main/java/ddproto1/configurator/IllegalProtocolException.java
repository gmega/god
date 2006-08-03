/*
 * Created on Oct 15, 2005
 * 
 * file: IllegalProtocolException.java
 */
package ddproto1.configurator;

public class IllegalProtocolException extends Exception{

    /**
     * 
     */
    private static final long serialVersionUID = 7173308095037284654L;

    public IllegalProtocolException() {
        super();
    }

    public IllegalProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalProtocolException(String message) {
        super(message);
    }

    public IllegalProtocolException(Throwable cause) {
        super(cause);
    }

}
