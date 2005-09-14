/*
 * Created on May 18, 2005
 * 
 * file: Stick.java
 */
package ddproto1.configurator.ioc.testclasses;

import java.util.Set;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.commons.IllegalAttributeException;

public class Stick implements IConfigurable{

    public void setAttribute(String key, String value) throws IllegalAttributeException {
        // TODO Auto-generated method stub
        
    }

    public String getAttribute(String key) throws IllegalAttributeException {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<String> getAttributeKeys() {
        return null;
    }

    public boolean isWritable() {
        // TODO Auto-generated method stub
        return false;
    }

}
