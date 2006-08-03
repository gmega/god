package ddproto1.exception.commons;

import java.util.List;





/*
 * Created on Jul 21, 2004
 *
 */

/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CommException extends NestedException{
    /**
     * 
     */
    private static final long serialVersionUID = 3617578219008571443L;
    public CommException(String s) { super(s); }
    public CommException(List   s) { super(s); }
    public CommException(String s, Exception e) { super(s,e); }
}
