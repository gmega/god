/*
 * Created on Apr 23, 2005
 * 
 * file: InvalidAttributeValueException.java
 */
package ddproto1.exception;

public class InvalidAttributeValueException extends Exception{

    private static final long serialVersionUID = 4049071636038760752L;

    public InvalidAttributeValueException() {
        super();
    }

    public InvalidAttributeValueException(String message) {
        super(message);
    }

}
