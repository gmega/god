/*
 * Created on Aug 2, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: HandlerException.java
 */

package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class HandlerException extends RuntimeException{
    /**
     * 
     */
    private static final long serialVersionUID = 3257291344119609398L;

    public HandlerException(String s){ super(s); }
}
