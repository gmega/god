/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Configurable.java
 */

package ddproto1.configurator;

import ddproto1.exception.IllegalAttributeException;

/**
 * @author giuliano
 *
 */
public interface IConfigurable extends IInfoCarrier {
    public void setAttribute(String key, String value) throws IllegalAttributeException;
}
