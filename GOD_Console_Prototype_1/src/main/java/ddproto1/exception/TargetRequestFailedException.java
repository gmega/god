/*
 * Created on Nov 24, 2005
 * 
 * file: TargetRequestFailedException.java
 */
package ddproto1.exception;

public class TargetRequestFailedException extends Exception{
    public TargetRequestFailedException() { super(); }
    public TargetRequestFailedException(String message, Throwable cause) { super(message, cause); }
    public TargetRequestFailedException(String message) { super(message);}
    public TargetRequestFailedException(Throwable cause) { super(cause);}
}
