/*
 * Created on Apr 23, 2005
 * 
 * file: InvalidAttributeValueException.java
 */
package ddproto1.exception.commons;



public class InvalidAttributeValueException extends AttributeAccessException{

    private static final long serialVersionUID = 8113839085172900353L;

    public InvalidAttributeValueException() {
        super();
    }

    public InvalidAttributeValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAttributeValueException(String message) {
        super(message);
    }

    public InvalidAttributeValueException(Throwable cause) {
        super(cause);
    }
 
}
