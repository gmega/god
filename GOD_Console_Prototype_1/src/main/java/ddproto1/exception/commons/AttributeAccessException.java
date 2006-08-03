/*
 * Created on Aug 2, 2005
 * 
 * file: AttributeException.java
 */
package ddproto1.exception.commons;

public class AttributeAccessException extends Exception{

    private static final long serialVersionUID = 8705050788900698193L;

    public AttributeAccessException() {
        super();
    }

    public AttributeAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public AttributeAccessException(String message) {
        super(message);
    }

    public AttributeAccessException(Throwable cause) {
        super(cause);
    }
}
