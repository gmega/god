/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: NoSuchSymbolException.java
 */

package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class NoSuchSymbolException extends Exception{
    /**
     * 
     */
    private static final long serialVersionUID = 4121129251467702832L;

    public NoSuchSymbolException(String s){super(s);}
}
