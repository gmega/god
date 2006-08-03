/*
 * Created on May 1, 2006
 * 
 * file: MultiMap.java
 */
package ddproto1.util.collection;

import java.util.Set;

public interface MultiMap<K, V> {

    /**
     * Adds the element 'val' to the first position of list 'key'.
     * 
     * @param key
     * @param val
     */
    public void add(K key, V val);

    /**
     * Returns an iterable from where all class keys can be
     * retrieved.
     * 
     * @return
     */
    public Set<K> keySet();

    /**
     * Returns the number of elements classified under key "key".
     * 
     * @param key
     * @return
     */
    public int size(K key);

    /**
     * Tests whether the given class exists.
     * 
     * @param key
     * @return
     */
    public boolean containsKey(K key);

    public boolean contains(K key, V val);

    /**
     * Removes element 'val' from class 'key'.
     * 
     * @param key
     * @param val
     * @return <b> true </b> if the element has been found and removed, <b> false </b> otherwise.
     */
    public boolean remove(K key, V val);

    /**
     * Returns an iterable that allows the user to iterate through all
     * elements of a given class.
     * 
     * @param key - The class.
     * @return
     */
    public Iterable<V> get(K key);

    /**
     * Removes an empty class.
     * 
     * @param key
     */
    public boolean removeClass(K key);

    /**
     * Clears all collection contents.
     *
     */
    public void clear();

}