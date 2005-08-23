/*
 * Created on Apr 23, 2005
 * 
 * file: BranchKey.java
 */
package ddproto1.configurator.newimpl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ddproto1.util.collection.AbstractMultiMap;


public class BranchKey <K, V>{
    
    public static final boolean IS_KEY = true;
    public static final boolean IS_VALUE = false;
    
    private K key;
    private V val;
    
    private int hashCode;
    
    private List<IRemovalStrategy> removalList = new LinkedList<IRemovalStrategy>();
    
    public BranchKey(K key, V val){
        this.key = key;
        this.val = val;
        this.hashCode = key.hashCode() + val.hashCode();
    }
    
    public K getKey() { return key; }
    
    public V getValue() { return val; }
    
    public void invalidate(){
        for(IRemovalStrategy strategy : removalList)
            strategy.remove();
    }
    
    public String toString(){
        return "<" + key + "," + val + ">";
    }
    
    public boolean isSatisfiedBy(Map<String, String> valueMap){
        String value = valueMap.get(key);
        if(value == null) return false;
        if(value.equals(val)) return true;
        return false;
    }
    
    public void addTo(Map target, Object parameter, boolean isKey){
        removalList.add(new MapRemovalStrategy(target, isKey, parameter));
    }
    
    public void addTo(Collection target){
        removalList.add(new CollectionRemovalStrategy(target));
    }
    
    public void addTo(AbstractMultiMap target, String classKey, boolean isKey){
        removalList.add(new MultiMapRemovalStrategy(target, classKey, isKey));
    }
    
    public int hashCode(){
        return hashCode;
    }
    
    public boolean equals(Object anObject){
        if(!(anObject instanceof BranchKey)) return false;
        BranchKey other = (BranchKey)anObject;
        return key.equals(other.key) && val.equals(other.val);
    }
    
    private interface IRemovalStrategy{
        public void remove();
    }
    
    private class MapRemovalStrategy implements IRemovalStrategy{
        
        private Map target;
        private boolean isKey;
        private Object parameter;

        public MapRemovalStrategy(Map target, boolean isKey, Object parameter){
            if(isKey) target.put(BranchKey.this, parameter);
            else target.put(parameter, BranchKey.this);
            this.target = target;
            this.isKey = isKey;
        }
        
        public void remove() {
            if(isKey) target.remove(BranchKey.this);
            else target.remove(parameter);
        }
        
    }
    
    private class CollectionRemovalStrategy implements IRemovalStrategy{
        
        private Collection which;
        
        public CollectionRemovalStrategy(Collection c){
            c.add(BranchKey.this);
            this.which = c;
        }

        public void remove() {
            assert(which.contains(BranchKey.this));
            which.remove(BranchKey.this);
        }
    }
    
    private class MultiMapRemovalStrategy implements IRemovalStrategy{
        
        private AbstractMultiMap which;
        private Object classKey;
        private boolean isKey;
        
        public MultiMapRemovalStrategy(AbstractMultiMap c, Object classKey, boolean isKey){
            if(isKey) c.add(BranchKey.this, classKey);
            else c.add(classKey, BranchKey.this);
            this.isKey = isKey;
        }

        public void remove() {
            if(isKey) which.removeClass(BranchKey.this);
            else which.remove(classKey, BranchKey.this);
        }
        
    }
}