/*
 * Created on Jun 5, 2006
 * 
 * file: ParseException.java
 */
package ddproto1.debugger.managing;

public class ParseException extends Exception {
    public ParseException() { super(); }
    public ParseException(String message, Throwable cause) { super(message, cause); }
    public ParseException(String message) { super(message); }
    public ParseException(Throwable cause) { super(cause); }
}
