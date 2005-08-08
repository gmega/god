/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: InfoCarrier.java
 */

package ddproto1.configurator;

import java.util.Set;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.UninitializedAttributeException;


/**
 * @author giuliano
 *
 */
public interface IConfigurable {
    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException;
    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException;
    /**
     * Returns the current attribute key set. Might change with time.  
     * 
     * @return
     */
    public Set <String> getAttributeKeys();
}
