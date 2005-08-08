/*
 * Created on Apr 22, 2005
 * 
 * file: UnsetAttributeException.java
 */
package ddproto1.exception;

public class UninitializedAttributeException extends AttributeAccessException {

    private static final long serialVersionUID = 3258132436020639283L;

    public UninitializedAttributeException() {
        super();
    }

    public UninitializedAttributeException(String message) {
        super(message);
    }

    public UninitializedAttributeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UninitializedAttributeException(Throwable cause) {
        super(cause);
    }
}
