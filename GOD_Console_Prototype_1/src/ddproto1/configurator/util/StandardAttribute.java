/*
 * Created on Apr 23, 2005
 * 
 * file: StandardAttribute.java
 */
package ddproto1.configurator.util;

import java.util.Set;

import ddproto1.configurator.newimpl.IAttribute;
import ddproto1.util.collection.ReadOnlyHashSet;

public class StandardAttribute implements IAttribute{
    
    private ReadOnlyHashSet <String> values;
    private String key;
    private int hashCode;
        
    public StandardAttribute(String key, Set <String> constrainedValues){
        this.key = key;
        this.values = (constrainedValues == ANY) ? ANY
                : new ReadOnlyHashSet<String>(constrainedValues);
    }

    public String attributeKey() {
        return key;
    }

    public boolean isAssignableTo(String value){
        if(values == null) return true;
        return values.contains(value);
    }
    
    public ReadOnlyHashSet<String> acceptableValues(){
        return values;
    }
    
    public int hashCode(){
        return hashCode;
    }
    
    public boolean equals(Object anObject){
        /** We could be less strict but we run the risk of losing symmetry */
        if(!(anObject instanceof StandardAttribute)) return false;
        
        StandardAttribute other = (StandardAttribute)anObject;
        
        /** Key is different, forget it. */
        if(!key.equals(other.key)) return false;
        
        /** Check for null symmetry. */
        if(!((values == null) ^ (other.values == null))) return false;
        
        /** It's symmetric and it's null. They're equal. */
        if(values == null) return true;
        
        /** It's symmetric but it's not null. They're value sets must be equal. */
        else if(values.equals(other.values)) return true;
        
        return false;
    }
}
