/*
 * Created on Mar 22, 2005
 * 
 * file: MultiMap.java
 */
package ddproto1.util.collection;

import java.util.Collection;
import java.util.List;

/**
 * 
 * MultiMap with ordered chains. Uses List interface.
 * 
 * @author giuliano
 *
 */
public class OrderedMultiMap <K, V> extends AbstractMultiMap <K, V>{

    private Class <? extends List> listType;
    
    public OrderedMultiMap(Class <? extends List> listType){ 
        this.listType = listType;
    }
    
    /**
     * Adds the element 'val' to the last position of list 'key'.
     * 
     * @param key
     * @param val
     */
    public void addLast(K key, V val){
        insert(key, val, size(key));
    }

    /**
     * Removes the first element of class (list) 'key'.
     * 
     * @param key
     * @return
     */
    public V remove(K key){
        return remove(key, 0);
    }
    
    /**
     * Removes the last element of class (list) 'key'.
     * 
     * @param key
     * @return
     */
    public V removeLast(K key){
        return remove(key, size(key));
    }

    /**
     * Inserts a new element under key 'key', at index 'idx'.
     * 
     * @param key
     * @param value
     * @param idx
     * 
     * @throws IndexOutOfBoundsException if the index specified in <code>idx > size(key)</code> or <code> idx < 0 </code> 
     */
    public void insert(K key, V value, int idx){
        List<V> elementList = null;
        
        if(super.containsKey(key))
            elementList = (List<V>)getClassInternal(key);
        else{
            elementList = (List<V>)addClass(key);
        }
        
        elementList.add(idx, value);
    }

    /**
     * Removes the element at index 'idx' under key 'key'.
     * 
     * @param key
     * @param value
     * @param idx
     * 
     * @throws IndexOutOfBoundsException if the index specified in <code>idx >= size(key)</code> or <code> idx < 0 </code> 
     */

    public V remove(K key, int idx){
        List<V> elementList = null;
        
        if(!super.containsKey(key))
            throw new IndexOutOfBoundsException("Invalid index " + idx);
        
        elementList = (List <V>) getClassInternal(key);
        V val = elementList.remove(idx);
        if(elementList.isEmpty()){
            removeClass(key);
        }
        
        return val;
    }

    @Override
    protected Class <? extends Collection> getConcreteType() {
        return listType;
    }
}
