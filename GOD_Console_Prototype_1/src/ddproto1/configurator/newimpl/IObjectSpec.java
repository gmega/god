/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;
import java.util.Map;

import ddproto1.configurator.IConfigurable;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.IllegalAttributeException;

/**
 * This type represents a metaobject holding information about real objects. 
 * It's used by the configuration subsystem to release the real objects from
 * having to configure their children. This localizes knowledge. 
 * 
 * It is designed to support lazy instantiation and to do several constraint
 * checks, including attribute values and relationship cardinalities. All 
 * IObjectSpec instances should belong to an IObjectSpecType.
 * 
 * @author giuliano
 *
 */
public interface IObjectSpec extends IConfigurable{

	public static final String CONTEXT_VALUE = "#context";
	
    /**
     * Adds a child object specification to the current specification. 
     * 
     * @param child
     * @throws IllegalAttributeException if there is a cardinality constraint 
     * violation or if the child's type is not supported by the current spec.
     */
    public void addChild(IObjectSpec child) throws IllegalAttributeException;
    
    /**
     * Removes the child passed as parameter from the current spec.
     * 
     * @param child
     * @return
     * @throws IllegalAttributeException if the child's type is not supported
     * by the current spec.
     */
    public boolean removeChild(IObjectSpec child) throws IllegalAttributeException;
    
    /**
     * Returns a list of all children specifications of the current spec.
     * 
     * @return
     */
    public List<IObjectSpec> getChildren();
        
    /**
     *   
     * 
     * @param type - alias of the interface set or name of the non-incarnable type.
     * @return
     */
    public List<IObjectSpec> getChildrenOfType(String type);

    public IObjectSpec getChildOfType(String type) throws AmbiguousSymbolException;
    
    /**
     * 
     * @param type
     * @return
     * @throws ClassNotFoundException
     */
    public List<IObjectSpec> getChildrenSupporting(String type) throws ClassNotFoundException;
    
    public List<IObjectSpec> getChildrenSupporting(Class type);
    
    public IObjectSpec getChildSupporting(Class type) throws AmbiguousSymbolException;
    
	public IObjectSpecType getType();
    
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
     * Returns a list of all required attributes that have not yet been assigned for this
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
    
}
