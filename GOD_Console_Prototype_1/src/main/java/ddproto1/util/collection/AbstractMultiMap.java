/*
 * Created on Apr 3, 2005
 * 
 * file: AbstractMultiMap.java
 */
package ddproto1.util.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ddproto1.exception.commons.NestedRuntimeException;

/**
 * This class implements a collection that resembles a map, but accepts multiple
 * insertions per key. It's actually a collection for classifying elements under
 * a common key and then accessing them. It's (deliberately) not thread safe,
 * though it will catch some forms of concurrent modification (such as trying to
 * iterate through a class of elements after it has been removed).
 * 
 * Don't trust it under multiple threads, please.
 * 
 * @author giuliano
 */

public abstract class AbstractMultiMap <K, V> implements MultiMap<K, V>{

    private Map<K, Collection<V>> listMap = new HashMap<K, Collection<V>>();
    private Map <K, InternalMultimapIterable> iterableCache = new HashMap <K, InternalMultimapIterable>(); 

    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#add(K, V)
     */
    public void add(K key, V val){
        if(!listMap.containsKey(key)) this.addClass(key).add(val);
        else listMap.get(key).add(val);
    }
    
    protected abstract Class <? extends Collection> getConcreteType();
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#keySet()
     */
    public Set<K> keySet(){
        return listMap.keySet();
    }    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#size(K)
     */
    public int size (K key){
        if(listMap.containsKey(key))
            return listMap.get(key).size();
        else
            return 0;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#containsKey(K)
     */
    public boolean containsKey(K key){
        return listMap.containsKey(key);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#contains(K, V)
     */
    public boolean contains(K key, V val){
        Collection <V> elements = listMap.get(key);
        if(elements == null)
            return false;
        
        return elements.contains(val);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#remove(K, V)
     */
    public boolean remove(K key, V val){
        if(!listMap.containsKey(key)) return false;
        return listMap.get(key).remove(val);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#get(K)
     */
    public Iterable<V> get(K key){
        return iterableCache.get(key);
    }
    
    protected Collection <V> getClassInternal(K key){
        return listMap.get(key);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#removeClass(K)
     */
    public boolean removeClass(K key){
        Collection<V> elementList = listMap.remove(key);
        if(elementList == null) return false;
        InternalMultimapIterable impi = iterableCache.remove(key);
        impi.invalidate();
        return true;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.util.collection.MultiMap#clear()
     */
    public void clear(){
        listMap.clear();
        // Invalidates all iterables that might be in use.
        for(InternalMultimapIterable iterable : iterableCache.values()){
            iterable.invalidate();
        }
        iterableCache.clear();
    }

    /**
     * Creates a new class for inserting elements. 
     * 
     * @param key
     * @return
     */
    protected Collection<V> addClass(K key){
        try{
            Collection<V> elementList = getConcreteType().newInstance();
            listMap.put(key, elementList);
            iterableCache.put(key, new InternalMultimapIterable(key, elementList));
            return elementList;
        }catch(Exception e){
            throw new NestedRuntimeException(e);
        }
    }

    /**
     * This internal class wraps each element list into an iterable that connects their
     * iterator remove method to the removal of the list from the list map once all elements 
     * from a given list are removed.
     * 
     * An iterable might be invalidated if the list becomes empty or it's removed by some
     * other reason. 
     * 
     * @author giuliano
     *
     */
    protected class InternalMultimapIterable implements Iterable<V>{
        
        private K associateKey;
        private Collection<V> associate;
        private boolean valid = true;
        
        private InternalMultimapIterable(K associateKey, Collection<V> associate){
            this.associate = associate;
            this.associateKey = associateKey;
        }

        public Iterator<V> iterator() {
            return new Iterator<V>(){
                
                private Iterator<V> trueIt = associate.iterator();

                public boolean hasNext() {
                    return trueIt.hasNext();
                }

                public V next() {
                    checkValid();
                    return trueIt.next();
                }

                public void remove() {
                    checkValid();
                    trueIt.remove(); 
                    if(associate.size() == 0)
                        removeClass(associateKey);
                }
                
            };
        }
        
        private void checkValid(){
            if(!valid) throw new IllegalStateException();
        }
        
        protected void invalidate(){
            valid = false;
        }

    }
}
