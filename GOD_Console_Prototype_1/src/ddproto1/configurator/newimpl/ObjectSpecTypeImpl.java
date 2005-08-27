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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import ddproto1.configurator.IReadableConfigurable;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InternalError;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.collection.OrderedMultiMap;
import ddproto1.util.collection.ReadOnlyHashSet;
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

    private static final IAttributeLookupAlgorithm DEFAULT_LOOKUP_STRATEGY = new DFSAttributeLookup();
    
    private String concreteType;
    private String interfaceType;
    private SpecLoader loader;
    
    /** Static constraint storage (local attributes and children). */
    private Map<String, IAttribute> requiredAttributes;
    private Map<String, IIntegerInterval> children;
    
    /** Stores all branch keys bound to a given attribute. */
    private UnorderedMultiMap<String, BranchKey> attributeBranches = new UnorderedMultiMap<String, BranchKey>(HashSet.class);
    
    /** Dynamic constraint storage (supertypes and their branch keys). */
    private Map<BranchKey, ObjectSpecTypeImpl> conditionalSpecs;
    
    /** Attribute lookup algorithm. */
    IAttributeLookupAlgorithm lookupStrategy;
    
    /** Optional children maps.
     * 
     * The first map relates the optional children types to their branch keys.
     * It's used for easily determining which branch keys are satisfied for a given
     * child type.
     * The second map relates branch keys to maps of child types, which in turn map
     * to the actual constraints. That way you can easily know which constraint is 
     * associated to a given child type under some branch key. 
     * 
     * */
    private UnorderedMultiMap <String, BranchKey> optionalChildren = new UnorderedMultiMap<String, BranchKey>(HashSet.class);
    private Map <BranchKey, Map<String, IIntegerInterval>> optionalChildrenQ = new HashMap<BranchKey, Map<String, IIntegerInterval>>();

    /** This is for a future notification channel */
    private List <IObjectSpecType> parentList = new LinkedList<IObjectSpecType>();
        
    /** Gutted WeakHashSet that keeps track of our instances. */
    private Set<WeakReference<ObjectSpecInstance>> spawn = new HashSet<WeakReference<ObjectSpecInstance>>();
    private ReferenceQueue rq = new ReferenceQueue();
    
    private ReadOnlyHashSet<Class> intfs;

    private ObjectSpecTypeImpl (){
        requiredAttributes = new HashMap<String, IAttribute>();
        children = new HashMap<String, IIntegerInterval>();
        conditionalSpecs = new HashMap<BranchKey, ObjectSpecTypeImpl>();
    }
       
    public ObjectSpecTypeImpl(String incarnableType, String type,
            SpecLoader loader, Set<Class> intfs){
        this(incarnableType, type, loader, intfs, null);
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
    public ObjectSpecTypeImpl(String incarnableType, String type,
            SpecLoader loader, Set<Class> intfs,
            IAttributeLookupAlgorithm lookupStrategy) {
        this();
        if (loader == null || type == null)
            throw new NullPointerException();
        this.intfs = new ReadOnlyHashSet<Class>(intfs);
        this.interfaceType = type;
        this.concreteType = incarnableType;
        this.loader = loader;
        if (lookupStrategy == null)
            this.lookupStrategy = ObjectSpecTypeImpl.DEFAULT_LOOKUP_STRATEGY;
    }
    
    /**
     * Adds a certain number of children to this specification. This number will be enforced
     * at all instances and can be modified later (check 
     * ObjectSpecTypeImpl$ObjectSpecInstance.updateChildList() for aditional semantics).
     * 
     */
    public void addChildConstraint(String childtype, int min, int max) {
        this.expungeStaleEntries();
        this.checkMinMax(min, max);
        children.put(childtype, new IntegerIntervalImpl(min, max));        
    }
    
    
    public void addOptionalChildrenConstraint(BranchKey precondition, String childtype, int min, int max)
        throws InvalidAttributeValueException, IllegalAttributeException
    {
        this.expungeStaleEntries();
        this.checkMinMax(min, max);
        /** Adding an optional constraint involves three steps
         * 1) Validating that the actual branch key is valid. */
        validateBranchKey(precondition);
         
        /** 2) Inserting the branch key that enables the constraint into the branch key 
         * mappings for the child type that's being constrained. */
        optionalChildren.add(childtype, precondition);
        /** 3) Inserting the actual constraint into the branch-key-to-child-type map */
        Map<String, IIntegerInterval> constraints = optionalChildrenQ.get(precondition);
        if(constraints == null){
            constraints = new HashMap<String, IIntegerInterval>();
            optionalChildrenQ.put(precondition, constraints);
        }

        /** NOTE! Adding a second constraint for the same precondition and under
         * the same childtype replaces the older constraint. That's because it doesn't
         * make any sense to specify two intervals for cardinality check for the same
         * child type. 
         */
        constraints.put(childtype, new IntegerIntervalImpl(min, max));
        
        /** Register this branch key as belonging to the attribute under it's key. 
         * We don't have to worry with duplicates because we're using a multimap with
         * sets. */
        attributeBranches.add(precondition.getKey(), precondition);
    }
    
    public ReadOnlyHashSet <Class> getSupportedInterfaces(){
        return intfs;
    }
    
    public boolean removeOptionalChildrenConstraint(BranchKey precondition, String childtype){
        this.expungeStaleEntries();
        /** 1) First remove the branch key from the child type brach key list */
        if(!optionalChildren.containsKey(childtype)) return false;
        optionalChildren.remove(childtype, precondition);
        /** 2) Now removes the interval associated to that child type under the 
         * mappings of the branch key index. Note that if there was a branch key
         * to be removed in (1), then there must be a child-type-to-interval mapping
         * to remove here. 
         */
        Map<String, IIntegerInterval> constraints = optionalChildrenQ.get(precondition);
        if(constraints == null) throw new InternalError("Internal assertion failure");
        if(constraints.containsKey(childtype)) constraints.remove(childtype);
        if(constraints.isEmpty()) optionalChildrenQ.remove(precondition);
        checkPurgeBranchKey(precondition);
        return true;
    }
    
    /**
     * Removes a given number of children. Children added by optional specifications
     * will not be removed.
     */
    public boolean removeChildConstraint(String childtype)
    {
        this.expungeStaleEntries();
        
        if(!children.containsKey(childtype)) return false;
        children.remove(childtype);
        return true;
    }
    
    private void checkMinMax(int min, int max){
        if(min < 0 || max < 0) throw new RuntimeException("The number of allowed children must be a positive integer.");
        if(max < min) throw new RuntimeException("Maximum number of allowed children must be at least as large as the minimum.");
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
            
            /** All branch keys that depend on this attribute must be invalidated.
             * This include removing all supertypes and optional children that 
             * depend on them. 
             * We can traverse the branch-key-to-constraint map to find out which 
             * children types are bound under this particular branch key.          */
            
            if(!attributeBranches.containsKey(attributeKey)) return true;
            for(BranchKey bk : attributeBranches.getClass(attributeKey)){
            	Map<String, IIntegerInterval> constraints = optionalChildrenQ.get(bk);
            	
            	boolean something = false;
 
            	/** Unbinds optional children */
            	if(constraints != null){
            		for(String childType : constraints.keySet()){
            			optionalChildren.remove(childType, bk);
            		}
            		optionalChildrenQ.remove(bk);
            		something = true;
            	}
            	
            	/** Unbinds optional supertypes */
            	if(conditionalSpecs.containsKey(bk)){
            		conditionalSpecs.remove(bk);
            		something = true;
            	}
            	
            	/** This branch key should either unbind supertypes or children, 
            	 * otherwise it should not be in the branch list for this attribute
            	 * on the first place.
            	 */
            	assert(something);
            }
            
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
        
        if(values == null) return false; // What the heck is this?
        
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
    
    public Set <String> getRestrictedKeys(Map<String,String> state){
        Set <String> keys = new HashSet<String>();
        keys.addAll(requiredAttributes.keySet());
        
        for(BranchKey branch : conditionalSpecs.keySet()){
            String val = state.get(branch.getKey());
            if(val == null) continue;
            if(val.equals(branch.getValue())){
                ObjectSpecTypeImpl childSpec = conditionalSpecs.get(branch);
                keys.addAll(childSpec.getRestrictedKeys(state));
            }
        }
        
        return keys;
    }
    
    public IObjectSpec makeInstance() throws InstantiationException {
        return new ObjectSpecInstance(this, this, lookupStrategy);
    }
    
    private void validateBranchKey(BranchKey bk) throws IllegalAttributeException, InvalidAttributeValueException{
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
    }

    public void bindOptionalSupertype(BranchKey bk, String concrete, String type)
        throws IllegalAttributeException,
        InvalidAttributeValueException, SpecNotFoundException, IOException,
        SAXException
    {
        this.expungeStaleEntries();
        this.validateBranchKey(bk);
        
        ObjectSpecTypeImpl spec = loader.specForName(concrete, type);
        
        /** We use our private protocol to add ourselves as parents 
         * to the newly created spec. */
        spec.addParent(this);   
        
        attributeBranches.add(bk.getKey(), bk);
        conditionalSpecs.put(bk, spec);
    }
    
    public void unbindOptionalSupertype(BranchKey bk) 
    {
        expungeStaleEntries();
        if(conditionalSpecs.containsKey(bk)){ conditionalSpecs.remove(bk); }
        checkPurgeBranchKey(bk);
    }
    
    private void checkPurgeBranchKey(BranchKey bk){
    	if(optionalChildrenQ.containsKey(bk) || conditionalSpecs.containsKey(bk)) return;
    	attributeBranches.remove(bk.getKey(), bk);
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
    
    public IIntegerInterval computeResultingChildrenConstraints(
            IObjectSpecType type, Map<String, String> attValues) {

        String xmlType;

        try {
            xmlType = type.getInterfaceType();
            return this.computeResultingChildrenConstraints(xmlType, attValues);
        } catch (IllegalAttributeException ex) {
            throw new NestedRuntimeException("Unexpected condition", ex);
        }

    }
    
    public IIntegerInterval computeResultingChildrenConstraints(
            String xmlType, Map<String, String> attValues) {
        
        int min = 0, max = 0;
        
        /** First, our own children. */
        IIntegerInterval our = children.get(xmlType);
        if(our != null){
            min += our.getMin();
            max += our.getMax();
        }
        
        /** Second, the optional supertype children */
        for (BranchKey bk : conditionalSpecs.keySet()) {
            /** Is the branch key satisfied? */
            if (!bk.isSatisfiedBy(attValues))
                continue;
            /** Well, it is. */
            ObjectSpecTypeImpl superType = conditionalSpecs.get(bk);
            IIntegerInterval superConstraints = superType
                    .computeResultingChildrenConstraints(xmlType, attValues);
            
            min += superConstraints.getMin();
            max += superConstraints.getMax();
        }
        
        /** And third, the optional children */
        Iterable <BranchKey>iterable = optionalChildren.getClass(xmlType);
        
        /** No optional children of this type. */
        if(iterable == null) return new IntegerIntervalImpl(min, max); 
        
        for(BranchKey bk : iterable){
            if(!bk.isSatisfiedBy(attValues)) continue;
            assert(optionalChildrenQ.containsKey(bk));
            Map<String, IIntegerInterval> part = optionalChildrenQ.get(bk);
            assert(part != null);
            IIntegerInterval optionalInterval = part.get(xmlType);
            if(optionalInterval == null) continue;
            min += optionalInterval.getMin();
            max += optionalInterval.getMax();
        }
        
        return new IntegerIntervalImpl(min, max);
    }
    
    public Set<String> allSupportedChildrenTypes(Map<String, String> attValues){
        Set<String> results = new HashSet<String>();

        /** First, adds all of our supported types. */
        results.addAll(children.keySet());
        
        /** Second, adds all supported types for optional specs. */
        for (BranchKey bk : conditionalSpecs.keySet()) {
            if (!bk.isSatisfiedBy(attValues))
                continue;
            ObjectSpecTypeImpl superType = conditionalSpecs.get(bk);
            results.addAll(superType.allSupportedChildrenTypes(attValues));
        }
        
        /** Third, adds optional children */
        for(String childType : optionalChildren.keySet()){
            Iterable<BranchKey> branches = optionalChildren.getClass(childType);
            /** One branch key is enough. */
            for(BranchKey bk : branches){
                if(bk.isSatisfiedBy(attValues)){
                    results.add(childType);
                    break;
                }
            }
        }
        
        return results;
    }
    
    /**
     * Computes the projection of the children tree, calculating all resulting
     * constraints. This is a pretty expensive operation and I haven't really
     * made a large effort to make it fast, so you shouldn't call it too often.
     * 
     * @return
     */
    public Map<String, IIntegerInterval> computeAllChildrenConstraints(Map<String, String> attValues){
        Map<String, IIntegerInterval> results = new HashMap<String, IIntegerInterval>();
        
        Set<String> allSupported = this.allSupportedChildrenTypes(attValues);
        
        for(String child : allSupported){
            results.put(child, this.computeResultingChildrenConstraints(child, attValues));
        }
        
        return results;
    }
    
    private void addParent(IObjectSpecType parent){
        parentList.add(parent);
    }
    
    private void removeParent(IObjectSpecType parent){
        parentList.remove(parent);
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
    
    public String toString(){
        StringBuffer asString = new StringBuffer();
        asString.append("<");
        asString.append((concreteType == null)?"<non-incarnable>":concreteType);
        asString.append(", ");
        asString.append((interfaceType == null)?"<unknown type>":interfaceType);
        asString.append(">");
        return asString.toString();
    }
    
    private class ObjectSpecInstance implements IObjectSpec, IContextSearchable,  Cloneable {

        private Map <String, String> attributeValues = new HashMap <String, String>();
        
        private OrderedMultiMap<String, IObjectSpec> children = new OrderedMultiMap<String, IObjectSpec>(
                LinkedList.class);
        
        private LinkedList<IObjectSpec> plainChildren = new LinkedList <IObjectSpec>();
        private ISpecQueryProtocol internal;
        private IObjectSpecType parentType;
        private IAttributeLookupAlgorithm lookupStrategy;
        
        private Set<IObjectSpec> parentSet = new HashSet<IObjectSpec>();
        
        private ObjectSpecInstance(IObjectSpecType parentType, ISpecQueryProtocol internal, IAttributeLookupAlgorithm algorithm){
            this.internal = internal;
            this.parentType = parentType;
            this.lookupStrategy = algorithm;
        }
        
        public void addAsParent(IObjectSpec parent) throws DuplicateSymbolException{
        	if(parentSet.contains(parent)) throw new DuplicateSymbolException("You cannot add a parent twice.");
        	parentSet.add(parent);
        }
        
        public void removeParent(IObjectSpec parent) throws NoSuchSymbolException{
        	if(!parentSet.contains(parent)) throw new NoSuchSymbolException("You cannot remove a parent that does not exist.");
        	parentSet.remove(parent);
        }
        
        public Set<IObjectSpec> getAllParents(){
            return parentSet;
        }
           
        public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
            try{
                /** InvalidAttributeValueException is never thrown if the value 
                 * passed on to checkPurge is nil */
                checkPurge(key, null);
            }catch(InvalidAttributeValueException ex) { }
            
            String value;
            
            /** Attribute is undefined. We must check wether there is a default value or not. */
            if(!attributeValues.containsKey(key)) {
                /** It's okay to do an unchecked 'get' because checkPurge already
                 * checked the key's validity in the context of our attribute values.
                 */
                IAttribute attribute = internal.getAttribute(key);
                value = attribute.defaultValue();
                if(value == null) throw new UninitializedAttributeException();
                /** Lazily assigns the default value. */
                attributeValues.put(key, value);
            }else{
                value = attributeValues.get(key);
            }
            
            /** Common value, just return the string. */
            if(value != IObjectSpec.CONTEXT_VALUE) return value;
            
            value = null;
            
            /** CONTEXT_VALUE means we should infer the attribute's value 
             * from our parent's lexical scopes.
             */
            for(IObjectSpec parent : lookupStrategy.iterableWithRoot(this)){
                try{
                    /** This could cascade to other searches, but the value must
                     * eventually be reached.
                     */
                    value = parent.getAttribute(key);
                }
                /** Exception means this parent doesn't define our attribute. */
                catch(IllegalAttributeException ex) { }
                
                break;
            }
            
            if(value == null) throw new IllegalAttributeException("Attribute " + key + " could not be found." );

            /** If the value extracted from context violates assignment constraints, 
             * throws an exception.
             */
            IAttribute attribute = internal.getAttribute(key);
            if(!attribute.isAssignableTo(value))
                throw new IllegalAttributeException("The value '" + value
                        + "' extracted from context does not "
                        + "match any of the values "
                        + attribute.acceptableValues()
                        + " declared for attribute " + attribute.attributeKey());
            
            return value;
        }
        
        

        public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
            checkPurge(key, val);
            attributeValues.put(key, val);
        }
        
        public boolean validate(){
            if (this.getUnassignedAttributeKeys().size() != 0)
                return false;
            
            Map<String, Integer> childrenConstraints = this.getMissingChildren();
            for(Integer val : childrenConstraints.values())
                if(val != 0) return false;
            
            return true;
        }
        
        public List<String> getUnassignedAttributeKeys(){
            Set<String> allKeys = internal.getRestrictedKeys(attributeValues);
            List<String> missing = new LinkedList<String>();
            for(String key : allKeys){
                if(checkPurgeKey(key) && !attributeValues.containsKey(key)){
                    /** Might be an uninitialized default attribute. */
                    IAttribute attribute = internal.getAttribute(key);
                    String value = attribute.defaultValue();
                    /** Lazily assigns the default value. */
                    if(value != null) attributeValues.put(key, value);
                    else missing.add(key);
                }
            }
            
            return missing;
        }
        
        public Map<String, Integer> getMissingChildren(){
        	
        	/** All children starts as a projection of all supported children types, 
        	 * collected from required and optional children and supertypes.
        	 */
            Set<String> allChildren = internal.allSupportedChildrenTypes(attributeValues);
            
            /** But we must also add the children we already have, since we might hold children
             * that are no longer supported by the type we belong to (because we support dynamic
             * changes to types).
             */
            allChildren.addAll(children.keySet());
                        
            Map<String, Integer> childrenStats = new HashMap<String, Integer>();
            
            /** For each of the supported children, get their cardinalities and test
             * against the number of inserted children.
             */ 
            for(String childType : allChildren){
                IIntegerInterval iv = internal.computeResultingChildrenConstraints(childType, attributeValues);
                int current = children.size(childType);
                int min = iv.getMin();
                int max = iv.getMax();
                /** This object spec can still accept children of type 'childType' */
                if(current <= max){
                    if(current < min) childrenStats.put(childType, min - current);
                    else childrenStats.put(childType, 0);
                }
                /** This object spec cannot hold anymore children of type 'childType'*/
                else{
                    childrenStats.put(childType, max - current);
                }
            }
            return childrenStats;
        }
        
        public Set <String> getAttributeKeys(){
            return internal.getRestrictedKeys(attributeValues);
        }
        
        private boolean checkPurgeKey(String key){
            if(!internal.containsAttribute(key, attributeValues)){
                if(attributeValues.containsKey(key)) attributeValues.remove(key);
                return false;
            }
            return true;
        }
        
        private void checkPurge(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException{
            if(!this.checkPurgeKey(key)){
                
                String concrete;
                
                throw new IllegalAttributeException("Illegal attribute '" + key
                        + "'. Specification instance type - " + parentType);
            }
            
            /** Context value is always valid. */
            if(val == IObjectSpec.CONTEXT_VALUE) return;
            
            if(val != null && !isAssignable(key, val, attributeValues))
                throw new InvalidAttributeValueException("Cannot assign " + val + " to attribute " + key);
        }
        
        public void addChild(IObjectSpec spec)
            throws IllegalAttributeException
        {
            String type = spec.getType().getInterfaceType();
            IIntegerInterval constraint = internal.computeResultingChildrenConstraints(type, attributeValues);
            int allowed = constraint.getMax();
            
            /** Checks if can add the child */
            if(allowed != INFINITUM && children.size(type) >= allowed)
                throw new IllegalAttributeException("Maximum number reached or unsupported children type " + type);
                                    
            children.insert(type, spec, 0);
            plainChildren.add(spec);
            
            /** Is this child context searchable? */
            if(spec instanceof IContextSearchable){
                IContextSearchable searchableChild = (IContextSearchable) spec;
                try{
                    searchableChild.addAsParent(this);
                }catch(DuplicateSymbolException ex){
                    throw new IllegalAttributeException(ex.getMessage(), ex);
                }
            }
        }
        
        public boolean removeChild(IObjectSpec spec) throws IllegalAttributeException{
            String actualType = spec.getType().getInterfaceType();
            if(!children.contains(actualType, spec)) return false;
            children.remove(actualType, spec);
            if(!plainChildren.contains(spec)) throw new InternalError("Internal inconsistency detected.");
            plainChildren.remove(spec);
            return true;
        }
        
        public List<IObjectSpec> getChildren() {
            LinkedList <IObjectSpec> copy = new LinkedList<IObjectSpec>();
            copy.addAll(plainChildren);
            return copy;
        }
        
        
        public IObjectSpecType getType() {
            return parentType;
        }

        public List<IObjectSpec> getChildrenSupporting(String type) 
            throws ClassNotFoundException
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class theClass = cl.loadClass(type);
            
            return this.getChildrenSupporting(theClass);
        }

        public List<IObjectSpec> getChildrenSupporting(Class type) {
            /**
             * We could index the children by supported interface using a MultiMap, 
             * but keeping the index would be more trouble than it's worth.
             */
            List <IObjectSpec> desiredChildren = new ArrayList<IObjectSpec>();
            for(IObjectSpec child : plainChildren){
                if(child.getType().getSupportedInterfaces().contains(type))
                    desiredChildren.add(child);
            }
            return desiredChildren;
        }
        
        public List<IObjectSpec> getChildrenOfType(String type){
            List<IObjectSpec> desiredChildren = new ArrayList<IObjectSpec>();
            for(IObjectSpec child : plainChildren){
                try{
                    if(child.getType().getInterfaceType().equals(type))
                        desiredChildren.add(child);
                }
                /** If the child doesn't declare an interface type than its
                 * interface type can't be what we're looking for.
                 */
                catch(IllegalAttributeException ex){ }
            }
            return desiredChildren;
        }
        
        public IObjectSpec getChildOfType(String type)
            throws AmbiguousSymbolException
        {
            List<IObjectSpec> childList = this.getChildrenOfType(type);
            if (childList.size() > 1)
                throw new AmbiguousSymbolException(
                        "There is more than one children supporting interface/class "
                                + type
                                + ". Use getChildOfType(String type) instead.");
            
            if(childList.size() == 0) return null;
            return childList.get(0);
        }
        

        public IObjectSpec getChildSupporting(Class type) throws AmbiguousSymbolException {
            List<IObjectSpec> children = this.getChildrenSupporting(type);
            if (children.size() > 1)
                throw new AmbiguousSymbolException(
                        "There is more than one children supporting interface/class "
                                + type
                                + ". Use getChildrenSupporting(Class type) instead."); 
            
            if(children.size() == 0) return null;
            return children.get(0);
        }
        
        public Object clone(){
        	throw new UnsupportedOperationException("Cloneable not available yet.");
        }
    }
    
}
