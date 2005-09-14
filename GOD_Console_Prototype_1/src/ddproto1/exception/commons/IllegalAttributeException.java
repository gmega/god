package ddproto1.exception.commons;




/*
 * Created on Jul 21, 2004
 *
 */

/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IllegalAttributeException extends AttributeAccessException{
    private static final long serialVersionUID = 3258417209599602997L;

    public IllegalAttributeException() {
        super();
    }

    public IllegalAttributeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalAttributeException(String message) {
        super(message);
    }

    public IllegalAttributeException(Throwable cause) {
        super(cause);
    }
}
