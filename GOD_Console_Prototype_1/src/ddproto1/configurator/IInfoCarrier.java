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


/**
 * @author giuliano
 *
 */
public interface IInfoCarrier {
    
    public void addAttribute(String key) throws IllegalAttributeException;
    public String getAttribute(String key) throws IllegalAttributeException;
    public Set getAttributesByGroup(String group); 
    public String [] getAttributeKeys();
}
