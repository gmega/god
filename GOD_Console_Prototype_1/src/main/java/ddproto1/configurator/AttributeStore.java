/*
 * Created on 19/07/2006
 * 
 * file: AttributeStore.java
 */
package ddproto1.configurator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.commons.IQueriableConfigurable;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;

/**
 * Small utility class that implements dynamic string attributes.
 * Could be modified for supporting multitype attributes.
 * 
 * @author giuliano
 *
 */
public class AttributeStore implements IQueriableConfigurable {
    private final Map<String, String> attributes = 
        Collections.synchronizedMap(new HashMap<String, String>());
    
    private Set<String> keys; 
    
    public AttributeStore(){
        setAttributeKeySet(attributes.keySet());
    }
    
    public void declareAttribute(String attKey){
        attributes.put(attKey, null);
    }
    
    public String getAttribute(String key)
        throws IllegalAttributeException, UninitializedAttributeException
    {
        if(!isValid(key))
            throw new IllegalAttributeException(key);
        String val = attributes.get(key);
        if(val == null)
            throw new UninitializedAttributeException(key);
        
        return val;
    }
    
    private synchronized void setAttributeKeySet(Set<String> keys){
        this.keys = keys;
    }

    public synchronized Set<String> getAttributeKeys() {
        return keys;
    }
    
    public boolean isValid(String attKey){
        return getAttributeKeys().contains(attKey);
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(!isValid(key))
            throw new IllegalAttributeException(key);
        attributes.put(key, val);
    }

    public boolean isWritable() { return true; }
}
