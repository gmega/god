/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: InfoCarrier.java
 */

package ddproto1.configurator.commons;

import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;


/**
 * @author giuliano
 *
 */
public interface IConfigurable {
    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException;
    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException;
    public boolean isWritable();
}
