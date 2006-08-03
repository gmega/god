/*
 * Created on Aug 3, 2005
 * 
 * file: ResourceLimitReachedException.java
 */
package ddproto1.exception;

public class ResourceLimitReachedException extends Exception {

    public ResourceLimitReachedException() {
        super();
    }

    public ResourceLimitReachedException(String message) {
        super(message);
    }

    public ResourceLimitReachedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceLimitReachedException(Throwable cause) {
        super(cause);
    }

}
