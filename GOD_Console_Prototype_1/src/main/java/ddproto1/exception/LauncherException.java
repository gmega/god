/*
 * Created on Jul 30, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: LauncherException.java
 */

package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class LauncherException extends Exception{
    /**
     * 
     */
    private static final long serialVersionUID = 3258133539961647925L;

    public LauncherException(String s){ super(s); }
    
    public LauncherException(String s, Throwable t) { super(s, t); }
   
    public LauncherException(Throwable t){ super(t);} 
}
