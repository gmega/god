package ddproto1.exception;
/*
 * Created on Jul 21, 2004
 *
 */

/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IllegalAttributeException extends Exception{
    /**
     * 
     */
    private static final long serialVersionUID = 3258417209599602997L;

    public IllegalAttributeException(Exception e) { super(e); }
    
    public IllegalAttributeException(String s){ super(s); }
}
