/*
 * Created on Feb 17, 2005
 * 
 * file: SafetyPropertyViolation.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class PropertyViolation extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 3256443586211035441L;

    /**
     * 
     */
    public PropertyViolation() {
        super();
    }

    /**
     * @param message
     */
    public PropertyViolation(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public PropertyViolation(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public PropertyViolation(Throwable cause) {
        super(cause);
    }

}
