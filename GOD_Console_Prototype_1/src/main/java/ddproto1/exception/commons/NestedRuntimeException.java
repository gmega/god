/*
 * Created on Sep 27, 2004
 * 
 * file: RuntimeWrapCapableException.java
 */
package ddproto1.exception.commons;

/**
 * @author giuliano
 */
public class NestedRuntimeException extends RuntimeException{
    /**
     * 
     */
    private static final long serialVersionUID = 3256446923350489136L;

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

}
