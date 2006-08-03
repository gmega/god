/*
 * Created on Apr 14, 2005
 * 
 * file: SpecNotFoundException.java
 */
package ddproto1.configurator;

public class SpecNotFoundException extends Exception {

    private static final long serialVersionUID = 3546361742859907639L;

    public SpecNotFoundException() {
        super();
    }

    public SpecNotFoundException(String message) {
        super(message);
    }

    public SpecNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpecNotFoundException(Throwable cause) {
        super(cause);
    }

}
