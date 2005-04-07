/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: AmbiguousSymbolException.java
 */

package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class AmbiguousSymbolException extends Exception{
    /**
     * 
     */
    private static final long serialVersionUID = 3761407521125906482L;

    public AmbiguousSymbolException(String s){ super(s);}
}
