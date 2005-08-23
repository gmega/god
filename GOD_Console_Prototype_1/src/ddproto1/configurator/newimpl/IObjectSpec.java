/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.IConfigurable;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.IllegalAttributeException;

public interface IObjectSpec extends IConfigurable{
    
    public void addChild(IObjectSpec child) throws IllegalAttributeException;
    
    public boolean removeChild(IObjectSpec child) throws IllegalAttributeException;
    
    /**
     * Convenience method. Returns whether this ObjectSpec has been fully initialized
     * (i.e. all of its attributes have been assigned) or not. If your spec type has
     * plenty of optionals this operation could turn out to be quite expensive, so
     * don't go using it into inner loops.  
     * 
     * @return <b>true</b> if every attribute has been assigned, <b>false</b> otherwise.
     */
    public boolean validate();
    
    /**
     * Returns a list of all required attributes that have not been assigned for this
     * object spec instance.
     * 
     * @return
     */
    public List<String> getUnassignedAttributeKeys();
    
    /**
     * Returns a map that relates missing children types to the ammount of children required.
     * This ammount is positive if the minimum cardinality constraint hasn't been reached, 
     * negative if the maximum cardinality constraint has been overflown, and zero if the 
     * cardinality constraint is satisfied. 
     * 
     * @return
     */
    public Map<String, Integer> getMissingChildren();
        
    public List<IObjectSpec> getChildren();
        
    public List<IObjectSpec> getChildrenOfType(String type) throws ClassNotFoundException;
    
    public List<IObjectSpec> getChildrenSupporting(Class type);
    
    public IObjectSpec getChildSupporting(Class type) throws AmbiguousSymbolException;
    
	public IObjectSpecType getType();
}
