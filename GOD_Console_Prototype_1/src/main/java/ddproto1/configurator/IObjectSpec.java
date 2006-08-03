/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;

import ddproto1.configurator.commons.IQueriableConfigurable;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;

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
public interface IObjectSpec extends IQueriableConfigurable, IAdaptable{

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
     * Gets all children that have 'type' as declared spec type.
     * 
     * @param type - alias of the interface set or name of the non-incarnable type.
     * @return
     */
    public List<IObjectSpec> getChildrenOfType(String type);

    /**
     * Convenience method - if there's only one children of type 'type', using
     * this method is more economical and less akward
     * 
     * @param type
     * @return
     * @throws AmbiguousSymbolException
     */
    public IObjectSpec getChildOfType(String type) throws AmbiguousSymbolException;
    
    /**
     * Gets 
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
     * Convenience method. Returns <b>true</b> if this ObjectSpec has been fully initialized
     * and all of its cardinality constraints are respected and <b>false</b> otherwise.  
     * Since the IObjectSpec might have to traverse the entire hetererarchy, this operation
     * can be very expensive.
     * 
     * @return <b>true</b> if the spec is valid, <b>false</b> otherwise.
     */
    public boolean validate();
    
    public boolean isContextAttribute(String attributeKey)
            throws IllegalAttributeException, UninitializedAttributeException;
    
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
    
    /**
     * Returns the ammount of children of type 'type' that can be added to this spec 
     * before generating an exception.
     * 
     */
    public int allowedChildrenOfType(String type);
    
    public boolean isEquivalentTo(IObjectSpec spec);
    
    /**
     * Sets a batch of attributes so that the user doesn't have to worry about a valid setup 
     * order. If this operation fails, then there's been an attribute conflict. If it succeeds,
     * then either there hasn't been an attribute conflict or some attributes have been trashed
     * because of the particular order in which they've been set. In either case, it's always
     * better to check attribute constraints before trying to batch-set them. 
     * 
     * @param attributeBatch
     * @throws AttributeAccessException
     * 
     * This operation is not currently implemented because it can't be supported reliably.
     */
//    public void batchedSetAttribute(Map<String, String> attributeBatch) throws AttributeAccessException;
}
