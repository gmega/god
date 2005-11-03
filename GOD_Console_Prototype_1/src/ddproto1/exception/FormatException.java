/*
 * Created on Oct 17, 2005
 * 
 * file: FormatException.java
 */
package ddproto1.exception;

public class FormatException extends Exception{
    private static final long serialVersionUID = -7028045174047591249L;
    public FormatException() { super(); }
    public FormatException(String message, Throwable cause) { super(message, cause); }
    public FormatException(String message) { super(message); }
    public FormatException(Throwable cause) { super(cause); }
}
