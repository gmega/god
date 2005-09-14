/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: UnsupportedException.java
 */

package ddproto1.exception.commons;

/**
 * @author giuliano
 *
 */
public class UnsupportedException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 3977857388911343670L;
    public UnsupportedException() { }
    public UnsupportedException(String s) { super(s); }
}
