/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: InfoCarrier.java
 */

package ddproto1.configurator;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;

/**
 * @author giuliano
 *
 */
public interface IConfigurable extends IInfoCarrier{
    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException;
}
