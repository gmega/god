/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpecType.java
 */
package ddproto1.configurator.newimpl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.util.collection.ReadOnlyHashSet;

/**
 * This interface describes an object specification type. An object specification
 * type is a configurable entity that will be used for constraint checking on its
 * spawn (instances). It describes which attributes are supported by each instance
 * and allows the definition of conditional attribute-based lookup.<BR>
 * <BR>
 * Though an object specification type might be associated to an actual object type,
 * that is not necessarily true. An<b> object specification type </b>describes its
 * instances in the following terms:
 *  
 * <ol>
 * <li> Supported attributes </li>
 * <li> Children specifications </li>
 * <li> Optional attribute sets </li>
 * </ol>
 * 
 * 
 * @author giuliano
 *
 */
public interface IObjectSpecType {
    /** Defines the name of the attribute where the concrete-type will be stored (if any) */
	public static final String CONCRETE_TYPE_ATTRIBUTE = "concrete-type";
    
    public static final int INFINITUM = Integer.MAX_VALUE;
	
    /** Adds a child to this specification type.
     * 
     * @param childtype
     * @param multiplicity
     */
	public void addChildConstraint(String childtype, int min, int max);
    
    /**
     * Removes a child from this specification type. Removes its associated cardinality
     * constraints as well. Note that this doesn't mean that this specification type won't
     * be accepting children of this type, as there may be an optional supertype which 
     * may accept them.
     * 
     * @param childtype
     * @param howMany how many children to remove.
     * @return <b>true</b> if all children were removed successfully, <b>false</b> otherwise. 
     */
    public boolean removeChildConstraint(String childtype);
    
    public void addOptionalChildrenConstraint(BranchKey precondition, String childtype, int min, int max);
    
    public boolean removeOptionalChildrenConstraint(BranchKey precondition, String childtype);
    
    public String getInterfaceType() throws IllegalAttributeException;
    
    public ReadOnlyHashSet<Class> getSupportedInterfaces();
    
    /** If this instance is incarnable (i.e. can be tranformed into an IConfigurable 
     * by a suitable service locator) this method will return the name of the class
     * to which this specification should be translated to. 
     * 
     * @return the actual incarnation type for this specification.
     * @throws IllegalAttributeException if the specification is not incarnable.
     */
    public String getConcreteType() throws IllegalAttributeException;
    
    /**
     * Adds an attribute. An attribute is a pair (k; {c1, c2, ..., cn}) where k
     * is a key and {c1,...,cn} is the set of allowed values. The constraint set 
     * IAttribute.ANY represents the set of all possible values.
     * 
     * @param attribute the IAttribute to be added.
     */
	public void addAttribute(IAttribute attribute) throws DuplicateSymbolException;
    
    /**
     * Returns the instance of IAttribute associated with a given key.
     * 
     * @param key
     * @return <b>null</b> if there's no attribute with key <b>key</b>
     */
    public IAttribute getAttribute(String key);
    
    /**
     * Removes the attribute whose key is given by the first parameter.
     * 
     * @param attributeKey the key to the attribute to be removed.
     * @return <b>true</b> if the attribute could be removed, <b>false</b> otherwise.
     */
    public boolean removeAttribute(String attributeKey);

    /**
     * Binds a specification to a particular value of a given attribute. This specification
     * will be loaded as part of the current specification whenever the specified attribute
     * is assigned to the given value.
     * 
     * @param bk branch key that specifies both the attribute and the value to bind to.
     * @param specType type of the specification to be loaded
     * @param specConcrete concrete type of the specification to be loaded
     * 
     * @throws IllegalAttributeException if the required attribute doesn't exist
     * @throws InvalidAttributeValueException if the attribute cannot be assigned to the value
     * specified in the branch key.
     */
    public void bindOptionalSupertype(BranchKey bk, String specType,
            String specConcrete) throws IllegalAttributeException,
            InvalidAttributeValueException, SpecNotFoundException, IOException,
            SAXException;
    
    /**
     * Unbinds the specification from a given attribute value.
     * 
     * @param attributeKey
     * @param value
     * 
     * @throws IllegalAttributeException if the required attribute doesn't exist
     * @throws InvalidAttributeException if the attribute cannot be assigned to the value
     * @throws NoSuchSymbolException if the attribute value is not part of the valid 
     * value table for the target attribute.
     */
    public void unbindOptionalSupertype(BranchKey bk)
            throws IllegalAttributeException, InvalidAttributeValueException,
            NoSuchSymbolException;
    
    
    /**
     * Returns the set of all the valid attribute keys for this object type specification.
     * 
     * @return
     */
	public Set <String> attributeKeySet();
    
    /**
     * Tests whether the this specification contains an attribute identified by the given 
     * key or not. 
     * 
     * @param key
     * @return
     */
	public boolean containsAttribute(String attributeKey);
    public IObjectSpec makeInstance() throws InstantiationException;
 }
