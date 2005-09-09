/*
 * Created on Apr 23, 2005
 * 
 * file: StandardAttribute.java
 */
package ddproto1.configurator.util;

import java.util.Set;

import ddproto1.configurator.newimpl.IAttribute;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.util.collection.ReadOnlyHashSet;

public class StandardAttribute implements IAttribute{
    
    private ReadOnlyHashSet <String> values;
    private String key;
    private String defaultValue;
    private int hashCode;
        
    public StandardAttribute(String key, String defaultValue, Set <String> constrainedValues)
        throws InvalidAttributeValueException
    {
        this.key = key;
        
        this.values = (constrainedValues == ANY) ? ANY
                : new ReadOnlyHashSet<String>(constrainedValues);
        
        if(defaultValue == null) return;
        
        /** There's a default value. We must test if it's valid or not. 
         * CONTEXT_VALUE deferrs the test because we cannot know which 
         * value that'll be.*/
        if(defaultValue != IObjectSpec.CONTEXT_VALUE && values != ANY && !values.contains(defaultValue))
            throw new InvalidAttributeValueException("Default value cannot be assigned because " +
                    "the specified value constraints conflict with it.");
        
        this.defaultValue = defaultValue;
        
        /** Computes the hash code. */
        hashCode = key.hashCode();
    }

    public String attributeKey() {
        return key;
    }

    public boolean isAssignableTo(String value){
        if(values == ANY) return true;
        return values.contains(value);
    }
    
    public ReadOnlyHashSet<String> acceptableValues(){
        return values;
    }

    public String defaultValue(){
        return defaultValue;
    }
    
    public int hashCode(){
        return hashCode;
    }
    
    public String toString(){
        return "Key: " + key;
    }
    
    public boolean equals(Object anObject){
        /** We could be less strict but we run the risk of losing symmetry */
        if(!(anObject instanceof StandardAttribute)) return false;
        
        StandardAttribute other = (StandardAttribute)anObject;
        
        /** Key is different, forget it. */
        if(!key.equals(other.key)) return false;
        
        /** Check for null symmetry. */
        if((values == null) ^ (other.values == null)) return false;
        
        /** It's symmetric and it's null. They're equal. */
        if(values == null) return true;
        
        /** It's symmetric but it's not null. They're value sets must be equal. */
        else if(values.equals(other.values)) return true;
        
        return false;
    }
}
