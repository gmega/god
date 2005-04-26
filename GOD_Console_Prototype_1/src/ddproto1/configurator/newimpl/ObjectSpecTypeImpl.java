/*
 * Created on Apr 14, 2005
 * 
 * file: ObjectSpecTypeImpl.java
 */
package ddproto1.configurator.newimpl;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import sun.security.krb5.internal.crypto.s;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.util.collection.ReadOnlyHashSet;
import ddproto1.util.collection.UnorderedMultiMap;

public class ObjectSpecTypeImpl implements IObjectSpecType{

    private String concreteType;
    private String interfaceType;
    private SpecLoader loader;
    
    /** Static constraint storage. */
    private Map<String, IAttribute> requiredAttributes;
    private Map<String, Integer> children;
    
    /** Dynamic constraint storage. */
    private Map<BranchKey, IObjectSpecType> conditionalSpecs;
    private Map<String, BranchKey> conditionalAttributes;
    
    private Set <String> allAttributeKeys;
    private Set <String> allAttributeKeysRO;
    
    /** Gutted WeakHashSet. */
    private Set<WeakReference<ObjectSpecInstance>> spawn = new HashSet<WeakReference<ObjectSpecInstance>>();
    private ReferenceQueue rq = new ReferenceQueue();

    private ObjectSpecTypeImpl (){
        allAttributeKeys = new HashSet<String>();
        allAttributeKeysRO = new ReadOnlyHashSet<String>(allAttributeKeys);
        requiredAttributes = new HashMap<String, IAttribute>();
        children = new HashMap<String, Integer>();
        conditionalSpecs = new HashMap<BranchKey, IObjectSpecType>();
        conditionalAttributes = new HashMap<String, BranchKey>();
    }
    
    public ObjectSpecTypeImpl(String incarnableType, String type, SpecLoader loader){
        this();
        this.interfaceType = type;
        this.concreteType = incarnableType;
        this.loader = loader;
    }
    
    public void addChild(String childtype, int multiplicity) {
        
        /* Dealing with twisted auto-boxing semantics. But it's cool. At
         * least cooler than manual boxing/unboxing. 
         */ 
        Integer existent = children.get(childtype);
        if(existent != null) existent += multiplicity;
        else existent = 0;
        
        children.put(childtype, existent);        
    }

    public boolean removeChild(String childtype) {
        return !(children.remove(childtype) == null);
    }

    public String getConcreteType() {
       return concreteType;
    }
    
    public String getInterfaceType(){
        return interfaceType;
    }
    
    public void addAttribute(IAttribute attribute) 
        throws DuplicateSymbolException
    {
        expungeStaleEntries();
        String key = attribute.attributeKey();
        if(requiredAttributes.containsKey(key))
            throw new DuplicateSymbolException("Attribute " + key + " already exists.");
        requiredAttributes.put(key, attribute);
        allAttributeKeys.add(key);
    }

    public boolean removeAttribute(String attributeKey) 
        throws IllegalAttributeException
    {
        expungeStaleEntries();
        if(!requiredAttributes.containsKey(attributeKey)) return false;
        requiredAttributes.remove(attributeKey);
        return true;
    }

    public Set<String> attributeKeySet() {
        return allAttributeKeysRO;
    }

    public boolean containsAttribute(String key) {
        return requiredAttributes.containsKey(key) | conditionalAttributes.containsKey(key);
    }

    public IObjectSpec makeInstance() throws InstantiationException {
        return new ObjectSpecInstance();
    }

    public void bindOptionalSet(BranchKey bk, String concrete, String type)
        throws IllegalAttributeException,
        InvalidAttributeValueException, SpecNotFoundException, IOException,
        SAXException
    {
        expungeStaleEntries();
        
        /** Finds the attribute to which the optional set will be bound to */
        String key = bk.getKey();
        String val = bk.getValue();
        IAttribute attrib = requiredAttributes.get(key);
        if (attrib == null)
            throw new IllegalAttributeException(
                    "Cannot bind specification to non-existent attribute "
                            + key);
        
        /** Checks out if the binding is valid */
        if (!attrib.isAssignableTo(val))
            throw new InvalidAttributeValueException("Attribute <" + key
                    + "> cannot be assigned to value <" + val + ">");
        
        IObjectSpecType spec = loader.specForName(concrete, type);
        
        boolean undo = false;
        List <String> undoList = new LinkedList<String>();
                
        try{
            /** 
             * Associates the optional attributes with their branch key. 
             * This is for facilitating lookups when validating attributes. */
            for(String attribute : spec.attributeKeySet()){
                if(allAttributeKeys.contains(attribute))
                    throw new IllegalAttributeException(
                            "An attribute with key "
                                + attribute
                                + " already exists within the current specification scope.");
            
                           
                undoList.add(attribute);
                conditionalAttributes.put(attribute, bk);
                allAttributeKeys.add(attribute);
            }
        

            /** Binds the branch key to the spec. */
            conditionalSpecs.put(bk, spec);
        }catch(IllegalAttributeException ex){
            undo = true;
            throw ex;
        }catch(RuntimeException ex){
            undo = true;
            throw ex;
        }finally{
            if(undo == true){
                for(String attribute : undoList){
                    if(conditionalAttributes.containsKey(attribute)) conditionalAttributes.remove(attribute);
                    allAttributeKeys.remove(attribute);
                }
            }
        }
    }

    public void unbindOptionalSet(BranchKey bk) 
    {
        expungeStaleEntries();
        if(conditionalSpecs.containsKey(bk)){ conditionalSpecs.remove(bk); }
    }
    
    private boolean containsAttribute(String key, Map <String, String> instanceVals){
        /** Verifies if the attribute is a base attribute. */
        if(requiredAttributes.containsKey(key)) return true;

        /** Could still be a branch attribute */
        if(conditionalAttributes.containsKey(key)){
            /** Checks if the branch condition is satisfied */
            BranchKey bk = conditionalAttributes.get(key);
            String value = instanceVals.get(bk.getKey());
            if(value == null) return false;
            if(value.equals(bk.getValue())) return true;
        }
        
        return false;
    }
    
    public IAttribute getAttribute(String key) throws IllegalAttributeException{
        
        /** Checks if it's a base attribute */
        IAttribute attrib = requiredAttributes.get(key);
        
        if(attrib != null) return attrib;
        
        /** It's not. Try to acquire the branch key for this attribute */
        BranchKey bk = conditionalAttributes.get(key);
        if(bk == null)
            throw new IllegalAttributeException("Could not find attribute " + key);
        
        /** Asks the surrogate spec to get it. */
        IObjectSpecType specType = conditionalSpecs.get(bk);
        
        /** Tail recursion. We could eliminate this but I'll only go through the
         * trouble if this proves to be too slow to bear. */
        return specType.getAttribute(key);
    }
    
    private boolean isAssignable(String key, String val, Map<String,String> assignedVals)
        throws IllegalAttributeException
    {
        if(!this.containsAttribute(key, assignedVals))
            throw new IllegalAttributeException();
        return this.getAttribute(key).isAssignableTo(val);
    }
    
    private void expungeStaleEntries(){
        Reference<ObjectSpecInstance> ref;
        while((ref = rq.poll()) != null){
            spawn.remove(ref);
        }
    }
    
    private class ObjectSpecInstance implements IObjectSpec {

        private Map <String, String> attributeValues = new HashMap <String, String>();
        private IObjectSpecType parentType;
        
        private ObjectSpecInstance(){
            this.parentType = ObjectSpecTypeImpl.this;
        }
           
        public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
            try{
                /** InvalidAttributeValueException is never thrown if the value 
                 * passed on to checkPurge is nil */
                checkPurge(key, null);
            }catch(InvalidAttributeValueException ex) { }
            
            if(!attributeValues.containsKey(key)) throw new UninitializedAttributeException();
            return attributeValues.get(key);
        }

        public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
            checkPurge(key, val);
            attributeValues.put(key, val);
        }
        
        private void checkPurge(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException{
            if(!parentType.containsAttribute(key)){
                if(attributeValues.containsKey(key)) attributeValues.remove(key);
                throw new IllegalAttributeException();
            }
            
            if(val != null && !isAssignable(key, val, attributeValues))
                throw new InvalidAttributeValueException("Cannot assign " + val + " to attribute " + key);
        }
        
        public List<IObjectSpec> getChildren() {
            // TODO Auto-generated method stub
            return null;
        }

        public IObjectSpecType getType() {
            return parentType;
        }

        public IObjectSpec clone() {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
