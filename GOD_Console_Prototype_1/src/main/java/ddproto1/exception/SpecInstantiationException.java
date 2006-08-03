/*
 * Created on Apr 12, 2005
 * 
 * file: SpecInstantiationException.java
 */
package ddproto1.exception;

public class SpecInstantiationException extends Exception {

    private static final long serialVersionUID = 4050477932868940081L;

    public SpecInstantiationException() {
        super();
    }

    public SpecInstantiationException(String message) {
        super(message); 
    }
  
    public SpecInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpecInstantiationException(Throwable cause) {
        super(cause);
    }

}
