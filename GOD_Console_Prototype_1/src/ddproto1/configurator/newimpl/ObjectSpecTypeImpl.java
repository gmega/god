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

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.util.collection.OrderedMultiMap;
import ddproto1.util.collection.UnorderedMultiMap;

/**
 * Standard object specification type implementation. Maintains consistency between
 * instances and base type, even if there are dynamic updates. It is <b>NOT</b> thread-safe;
 * that is, you cannot expect for things to be consistent if you're accessing instances and
 * modifying the base type at the same time. 
 * 
 * @author giuliano
 *
 */
public class ObjectSpecTypeImpl implements IObjectSpecType, ISpecQueryProtocol{

    private String concreteType;
    private String interfaceType;
    private SpecLoader loader;
    
    /** Static constraint storage (local attributes and children). */
    private Map<String, IAttribute> requiredAttributes;
    private Map<String, Integer> children;
    
    /** Dynamic constraint storage (supertypes and their branch keys). */
    private Map<BranchKey, ObjectSpecTypeImpl> conditionalSpecs;
    
    /** Optional children */
    private UnorderedMultiMap <String, BranchKey> optionalChildren = new UnorderedMultiMap<String, BranchKey>(HashSet.class);
    private Map <BranchKey, Integer> optionalChildrenQ = new HashMap<BranchKey, Integer>();
        
    /** This is for a future notification channel */
    private List <IObjectSpecType> parentList = new LinkedList<IObjectSpecType>();
        
    /** Gutted WeakHashSet that keeps track of our instances. */
    private Set<WeakReference<ObjectSpecInstance>> spawn = new HashSet<WeakReference<ObjectSpecInstance>>();
    private ReferenceQueue rq = new ReferenceQueue();

    private ObjectSpecTypeImpl (){
        requiredAttributes = new HashMap<String, IAttribute>();
        children = new HashMap<String, Integer>();
        conditionalSpecs = new HashMap<BranchKey, ObjectSpecTypeImpl>();
    }
    
    /**
     * Constructor for class ObjectSpecTypeImpl.
     * 
     * @param incarnableType concrete type this spec represents (if any).
     * @param type type of this specification.
     * @param loader the specification loader this instance should use to locate other specifications.
     * 
     * @throws NullPointerException if parameter <b>type</b> or parameter <b>loader</b> is null.
     */
    public ObjectSpecTypeImpl(String incarnableType, String type, SpecLoader loader){
        this();
        if(loader == null || type == null) throw new NullPointerException();
        this.interfaceType = type;
        this.concreteType = incarnableType;
        this.loader = loader;
    }
    
    /**
     * Adds a certain number of children to this specification. This number will be enforced
     * at all instances and can be modified later (check 
     * ObjectSpecTypeImpl$ObjectSpecInstance.updateChildList() for aditional semantics).
     * 
     */
    public void addChild(String childtype, int multiplicity) {
        
        checkPositive(multiplicity, "Multiplicity");
        
        /* Dealing with twisted auto-boxing semantics. But it's cool. At
         * least cooler than manual boxing/unboxing. 
         */ 
        Integer existent = children.get(childtype);
        if(existent != null) existent += multiplicity;
        else existent = 0;
        
        children.put(childtype, existent);        
    }
    
    public void addOptionalChildren(BranchKey precondition, String childtype, int multiplicity){
        checkPositive(multiplicity, "Multiplicity");
        this.alterOptionalChildren(precondition, childtype, multiplicity, false);
    }
    
    public boolean removeOptionalChildren(BranchKey precondition, String childtype, int multiplicity){
        checkPositive(multiplicity, "Multiplicity");
        if(!optionalChildren.containsKey(childtype))return false;
        this.alterOptionalChildren(precondition, childtype, multiplicity, true);
        return true;
    }
    
    private void alterOptionalChildren(BranchKey bk, String cType, int number, boolean remove){
        Integer howMany = optionalChildrenQ.get(bk);
        howMany = (howMany == null)?0:howMany;
        
        /** Remove an infinite number of children means remove all. */
        if(number == INFINITUM){
            if(remove) howMany = -15; /** Make shure this value is different from INFINITUM */
            else howMany = INFINITUM;
        }else{
            howMany += number;
        }
        
        /** If remanining children is positive, set it to be the number of 
         * currently allowed children for the specific type being manipulated */
        if(howMany > 0 || howMany == INFINITUM){
            optionalChildren.add(cType, bk);
            optionalChildrenQ.put(bk, howMany);
        }
        
        /** Otherwise we must remove them from the optional list. */
        else{
            /** The only way this could fail is if the user asked to remove
             * something that doesn't exist.*/
            assert(optionalChildren.contains(cType, bk));
            assert(optionalChildrenQ.containsKey(cType));
            optionalChildren.remove(cType, bk);
            optionalChildrenQ.remove(cType);
        }   
        
    }

    private void checkPositive(int val, String what){
        if(val < 0 && val != INFINITUM) throw new RuntimeException(what + " must be a positive integer.");
    }
    
    /**
     * Removes a given number of children. Children added by optional specifications
     * will not be removed.
     * 
     */
    public boolean removeChild(String childtype, int howMany)
    {
        Integer current = children.get(childtype);
        if(current == null) return false;
            
        current -= howMany;
        if(current <= 0) children.remove(childtype);
        else children.put(childtype, current);
            
        return true;
    }


    public String getConcreteType() 
        throws IllegalAttributeException
    {
        if(concreteType == null) throw new IllegalAttributeException("This specification is not incarnable.");
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
        if(this.containsAttribute(key))
            throw new DuplicateSymbolException("Attribute " + key + " already exists.");
        requiredAttributes.put(key, attribute);
    }

    public boolean removeAttribute(String attributeKey) 
    {
        boolean removed = false;
        
        expungeStaleEntries();
        
        /** The attribute is ours. */
        if(requiredAttributes.containsKey(attributeKey)){
            requiredAttributes.remove(attributeKey);
            return true;
        }
        /** Look up the chain. */
        else{
            for(IObjectSpecType childSpec : conditionalSpecs.values()){
                removed |= childSpec.removeAttribute(attributeKey);
                if(removed == true) return true;
            }
        }
        return false;
    }

    public Set<String> attributeKeySet() {
        Set <String> fullSet = new HashSet<String>();
        
        fullSet.addAll(requiredAttributes.keySet());
        
        /** Everybody up the chain should also be included. */
        for(IObjectSpecType childSpec : conditionalSpecs.values()){
            fullSet.addAll(childSpec.attributeKeySet());
        }
        
        return fullSet;
    }

    public boolean containsAttribute(String key) {
        return containsAttribute(key, null);
    }
    
    
    public boolean containsAttribute(String key, Map<String, String> values){
        /** Maybe the attribute is local */
        if(requiredAttributes.containsKey(key))
            return true;
        
        if(values == null) return false;
        
        /** Nope. Will have to look up down the child chain. */
        for(BranchKey branch : conditionalSpecs.keySet()){
            String val = values.get(branch.getKey());
            if(val == null) continue;
            if(val.equals(branch.getValue())){
                ObjectSpecTypeImpl childSpec = conditionalSpecs.get(branch);
                if(childSpec.containsAttribute(key, values)) return true;
            }
        }
        
        /** No attribute named 'key'. */
        return false;
    }
    
    public IObjectSpec makeInstance() throws InstantiationException {
        return new ObjectSpecInstance(this, this);
    }

    public void bindOptionalSupertype(BranchKey bk, String concrete, String type)
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
        
        /** Checks out if the binding is valid (assumption is that the attribute has already been added) */
        if (!attrib.isAssignableTo(val))
            throw new InvalidAttributeValueException("Attribute <" + key
                    + "> cannot be assigned to value <" + val + ">");
        
        ObjectSpecTypeImpl spec = loader.specForName(concrete, type);
        
        boolean undo = false;
        
        /** We use our private protocol to add ourselves as parents 
         * to the newly created spec. */
        spec.addParent(this);   
        
        conditionalSpecs.put(bk, spec);
    }
    
    

    public void unbindOptionalSupertype(BranchKey bk) 
    {
        expungeStaleEntries();
        if(conditionalSpecs.containsKey(bk)){ conditionalSpecs.remove(bk); }
    }
    
    public IAttribute getAttribute(String key){
        
        /** Checks if it's a base attribute */
        IAttribute attrib = requiredAttributes.get(key);
        
        if(attrib != null) return attrib;
        
        for(IObjectSpecType surrogates : conditionalSpecs.values()){
            if((attrib = surrogates.getAttribute(key)) != null) return attrib;
        }
        
        return null;
    }
    
    public int childrenNumber(IObjectSpecType type, Map <String, String> attValues){
        int _realMany;
        String _type;
        try{
            _type = type.getInterfaceType();
            Integer howMany = children.get(_type);
            _realMany = (howMany == null)?0:howMany;
            
            /** INFINITUM overcomes whathever result there might be. */
            if(_realMany == INFINITUM) return INFINITUM;
        }catch(IllegalAttributeException ex){
            throw new NestedRuntimeException("Unexpected condition", ex);
        }
        
        /** First we count the required */
        for(ObjectSpecTypeImpl childSpec : conditionalSpecs.values()){
            int parcel = childSpec.childrenNumber(type, attValues);
            
            /** INFINITUM overcomes whathever result there might be. */
            if(parcel == INFINITUM) return INFINITUM;
            _realMany += parcel;
        }
        
        /** Now we count the optionals. */
        Iterable <BranchKey>iterable = optionalChildren.getClass(_type);
        if(iterable == null) return _realMany;
        
        for(BranchKey bk : iterable){
            String bKey = bk.getKey();
            String bVal = bk.getValue();
            String rVal = attValues.get(bKey);
            if(rVal == null) continue;
            if(rVal.equals(bVal)){
                assert(optionalChildrenQ.containsKey(bk));
                int parcel = optionalChildrenQ.get(bk);
                if(parcel == INFINITUM) return INFINITUM;
                _realMany += parcel;
            }
        }
        
        return _realMany;
    }
    
    private void addParent(IObjectSpecType parent){
        parentList.add(parent);
    }
    
    private void removeParent(IObjectSpecType parent){
        parentList.add(parent);
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
        private OrderedMultiMap<IObjectSpecType, IObjectSpec> children = new OrderedMultiMap<IObjectSpecType, IObjectSpec>(
                LinkedList.class);
        private LinkedList<IObjectSpec> plainChildren = new LinkedList <IObjectSpec>();
        private ISpecQueryProtocol internal;
        private IObjectSpecType parentType;
        
        private ObjectSpecInstance(IObjectSpecType parentType, ISpecQueryProtocol internal){
            this.internal = internal;
            this.parentType = parentType;
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
            if(!internal.containsAttribute(key, attributeValues)){
                if(attributeValues.containsKey(key)) attributeValues.remove(key);
                throw new IllegalAttributeException();
            }
            
            if(val != null && !isAssignable(key, val, attributeValues))
                throw new InvalidAttributeValueException("Cannot assign " + val + " to attribute " + key);
        }
        
        public void addChild(IObjectSpec spec)
            throws IllegalAttributeException
        {
            IObjectSpecType type = spec.getType();
            String childType = type.getInterfaceType();
            int allowed = internal.childrenNumber(type, attributeValues);

            updateChildList(spec, allowed);
            
            /** Checks if can add the child */
            if(allowed != INFINITUM && children.size(type) == allowed)
                throw new IllegalAttributeException("Maximum number reached or unsupported children type " + type.getInterfaceType());
            
            children.insert(type, spec, 0);
            plainChildren.add(spec);
        }
        
        
        public List<IObjectSpec> getChildren() {
            LinkedList <IObjectSpec> copy = new LinkedList<IObjectSpec>();
            copy.addAll(plainChildren);
            return plainChildren;
        }
        
        
        /**
         * This method lazily updates the children lists according to what is 
         * currently allowed by the specification.
         *  
         * @param spec
         * @throws IllegalAttributeException
         */
        private void updateChildList(IObjectSpec spec, int allowed)
            throws IllegalAttributeException
        {

            IObjectSpecType type = spec.getType();
            int toRemove = Math.max(0, children.size(type) - allowed);
            for (int i = 0; i < toRemove; i++) {
                plainChildren.remove(children.remove(type, 0));
            }
        }
        
        public IObjectSpecType getType() {
            return parentType;
        }
    }
}
