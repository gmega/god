/*
 * Created on Apr 23, 2005
 * 
 * file: IAttribute.java
 */
package ddproto1.configurator.newimpl;

import java.util.HashSet;
import java.util.Set;

import ddproto1.util.collection.ReadOnlyHashSet;

/**
 * This interface represents the interface that should be exposed by an attribute.
 * An important part of the attribute interface contract is that <b>attributes must  
 * be immutable</b>. 
 * 
 * @author giuliano
 */
public interface IAttribute {
    
    public static final ReadOnlyHashSet<String> ANY = new ReadOnlyHashSet<String>(
            new HashSet<String>());
    
    /**
     * Returns the key that identifies this attribute. Keys
     * should be unique inside the scope of a given specification.
     * 
     * @return the attribute's unique key.
     */
    public String attributeKey();
    
    /**
     * Answers whether this attribute can accept the String val as its
     * value or not.
     * 
     * @param val value of the attribute to be tested
     * @return the decision
     */
    public boolean isAssignableTo(String val);
    
    public Set<String> acceptableValues();    
}
