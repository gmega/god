package ddproto1.exception;

import java.util.List;

import ddproto1.exception.commons.NestedException;


/*
 * Created on Jul 21, 2004
 *
 */

/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConfigException extends NestedException{
    /**
     * 
     */
    private static final long serialVersionUID = 3832904372890711857L;
    public ConfigException(Exception ex){ super(ex); }
    public ConfigException(String s) { super(s); }
    public ConfigException(List   s) { super(s); }
    public ConfigException(String s, Exception e) {
        super(s, e);
    }
}
