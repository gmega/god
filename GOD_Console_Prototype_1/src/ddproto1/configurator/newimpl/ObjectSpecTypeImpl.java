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

import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.UninitializedAttributeException;
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

    private String concreteType;
    private String interfaceType;
    private SpecLoader loader;
    
    /** Static constraint storage (local attributes and children). */
    private Map<String, IAttribute> requiredAttributes;
    private Map<String, IntegerInterval> children;
    
    /** Dynamic constraint storage (supertypes and their branch keys). */
    private Map<BranchKey, ObjectSpecTypeImpl> conditionalSpecs;
    
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
    private Map <BranchKey, Map<String, IntegerInterval>> optionalChildrenQ = new HashMap<BranchKey, Map<String, IntegerInterval>>();
        
    /** This is for a future notification channel */
    private List <IObjectSpecType> parentList = new LinkedList<IObjectSpecType>();
        
    /** Gutted WeakHashSet that keeps track of our instances. */
    private Set<WeakReference<ObjectSpecInstance>> spawn = new HashSet<WeakReference<ObjectSpecInstance>>();
    private ReferenceQueue rq = new ReferenceQueue();
    
    private ReadOnlyHashSet<Class> intfs;

    private ObjectSpecTypeImpl (){
        requiredAttributes = new HashMap<String, IAttribute>();
        children = new HashMap<String, IntegerInterval>();
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
    public ObjectSpecTypeImpl(String incarnableType, String type, SpecLoader loader, Set<Class> intfs){
        this();
        if(loader == null || type == null) throw new NullPointerException();
        this.intfs = new ReadOnlyHashSet<Class>(intfs);
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
    public void addChildConstraint(String childtype, int min, int max) {
        checkMinMax(min, max);
        children.put(childtype, new IntegerInterval(min, max));        
    }
    
    
    public void addOptionalChildrenConstraint(BranchKey precondition, String childtype, int min, int max){
        checkMinMax(min, max);
        /** Adding an optional constraint involves two steps 
         * 1) Inserting the branch key that enables the constraint into the branch key 
         * mappings for the child type that's being constrained. */
        optionalChildren.add(childtype, precondition);
        /** 2) Inserting the actual constraint into the branch-key-to-child-type map */
        Map<String, IntegerInterval> constraints = optionalChildrenQ.get(childtype);
        if(constraints == null){
            constraints = new HashMap<String, IntegerInterval>();
            optionalChildrenQ.put(precondition, constraints);
        }
        
        /** NOTE! Adding a second constraint for the same precondition and under
         * the same childtype replaces the older constraint. That's because it doesn't
         * make any sense to specify two intervals for cardinality check for the same
         * child type. 
         */
        constraints.put(childtype, new IntegerInterval(min, max));
    }
    
    public ReadOnlyHashSet <Class> getSupportedInterfaces(){
        return intfs;
    }
    
    public boolean removeOptionalChildrenConstraint(BranchKey precondition, String childtype){
        /** 1) First remove the branch key from the child type brach key list */
        if(!optionalChildren.containsKey(childtype)) return false;
        optionalChildren.remove(childtype, precondition);
        /** 2) Now removes the interval associated to that child type under the 
         * mappings of the branch key index. Note that if there was a branch key
         * to be removed in (1), then there must be a child-type-to-interval mapping
         * to remove here. 
         */
        Map<String, IntegerInterval> constraints = optionalChildrenQ.get(precondition);
        if(constraints == null) throw new InternalError("Internal assertion failure");
        if(constraints.containsKey(childtype)) constraints.remove(childtype);
        if(constraints.isEmpty()) optionalChildrenQ.remove(precondition);
        return true;
    }
    
    /**
     * Removes a given number of children. Children added by optional specifications
     * will not be removed.
     * 
     */
    public boolean removeChildConstraint(String childtype)
    {
        if(!children.containsKey(childtype)) return false;
        children.remove(childtype);
        return true;
    }
    
    private void checkMinMax(int min, int max){
        if(min < 0 || max < 0) throw new RuntimeException("The number of allowed children must be a positive integer.");
        if(min < max) throw new RuntimeException("Maximum number of allowed children must be at least as large as the minimum.");
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
    
    public IntegerInterval computeResultingChildrenConstraints(
            IObjectSpecType type, Map<String, String> attValues) {
        
        int min = 0, max = 0;
        String xmlType;
                
        try{
            xmlType = type.getInterfaceType();
        }catch(IllegalAttributeException ex){
            throw new NestedRuntimeException("Unexpected condition", ex);
        }
        
        /** First, our own children. */
        IntegerInterval our = children.get(type);
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
            IntegerInterval superConstraints = superType
                    .computeResultingChildrenConstraints(type, attValues);
            
            min += superConstraints.getMin();
            max += superConstraints.getMax();
        }
        
        /** And third, the optional children */
        Iterable <BranchKey>iterable = optionalChildren.getClass(xmlType);
        
        /** No optional children of this type. */
        if(iterable == null) return new IntegerInterval(min, max); 
        
        for(BranchKey bk : iterable){
            if(!bk.isSatisfiedBy(attValues)) continue;
            assert(optionalChildrenQ.containsKey(bk));
            Map<String, IntegerInterval> part = optionalChildrenQ.get(bk);
            assert(part != null);
            IntegerInterval optionalInterval = part.get(xmlType);
            if(optionalInterval == null) continue;
            min += optionalInterval.getMin();
            max += optionalInterval.getMax();
        }
        
        return new IntegerInterval(min, max);
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
        
        public boolean isFullyInitialized(){
            return this.getAttributeKeys().size() == attributeValues.size();
        }
        
        public Set <String> getAttributeKeys(){
            return internal.getRestrictedKeys(attributeValues);
        }
        
        private void checkPurge(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException{
            if(!internal.containsAttribute(key, attributeValues)){
                if(attributeValues.containsKey(key)) attributeValues.remove(key);
                throw new IllegalAttributeException("Illegal attribute " + key
                        + ". Specification instance type - <"
                        + parentType.getConcreteType() + ", "
                        + parentType.getInterfaceType() + ">");
            }
            
            if(val != null && !isAssignable(key, val, attributeValues))
                throw new InvalidAttributeValueException("Cannot assign " + val + " to attribute " + key);
        }
        
        public void addChild(IObjectSpec spec)
            throws IllegalAttributeException
        {
            IObjectSpecType type = spec.getType();
            IIntegerInterval constraint = internal.childrenNumber(type, attributeValues);
            int allowed = constraint.getMax();
            updateChildList(spec, constraint.getMax());
            
            /** Checks if can add the child */
            if(allowed != INFINITUM && children.size(type) == allowed)
                throw new IllegalAttributeException("Maximum number reached or unsupported children type " + type.getInterfaceType());
            
            children.insert(type, spec, 0);
            plainChildren.add(spec);
        }
        
        
        public List<IObjectSpec> getChildren() {
            LinkedList <IObjectSpec> copy = new LinkedList<IObjectSpec>();
            copy.addAll(plainChildren);
            return copy;
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
            /** Infinite children allowed. Nothing to update. */
            if(allowed == INFINITUM) return; 
            
            IObjectSpecType type = spec.getType();
            int toRemove = Math.max(0, children.size(type) - allowed);
            for (int i = 0; i < toRemove; i++) {
                plainChildren.remove(children.remove(type, 0));
            }
        }
        
        public IObjectSpecType getType() {
            return parentType;
        }

        public List<IObjectSpec> getChildrenOfType(String type) 
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
    }
    
    private class IntegerInterval implements IIntegerInterval{
        private int min;
        private int max;
        
        public IntegerInterval(int min, int max){
            this.min = min;
            this.max = max;
        }
        
        public int getMin(){
            return min;
        }
        
        public int getMax(){
            return max;
        }
    }
}
