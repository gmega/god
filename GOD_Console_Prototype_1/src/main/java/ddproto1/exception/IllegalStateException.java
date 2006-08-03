/*
 * Created on Aug 16, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IllegalStateException.java
 */

package ddproto1.exception;

/**
 * RuntimeException counterpart to the non-runtime exception 
 * <i> InvalidStateException </i>.
 * 
 * 
 * @author giuliano
 *
 */
public class IllegalStateException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 4123387618353296690L;

    public IllegalStateException(String reason){super(reason);}
    
    public IllegalStateException(Exception ex){
        super(ex);
    }
}
