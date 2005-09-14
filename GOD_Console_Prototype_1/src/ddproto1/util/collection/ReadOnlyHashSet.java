/*
 * Created on Apr 24, 2005
 * 
 * file: ReadOnlyHashSet.java
 */
package ddproto1.util.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import ddproto1.exception.commons.UnsupportedException;

/**
 * Simple decorator that denies access to all operations that perform modifications on
 * the surrogate set. 
 * 
 * @author giuliano
 *
 */
public class ReadOnlyHashSet <T> implements Set <T> {
    
    private Set <T>surrogate;

    private ReadOnlyHashSet() { }
    
    public ReadOnlyHashSet (Set <T> surrogate){
        this.surrogate = surrogate;
    }
    
    public int size() {
        return surrogate.size();
    }

    public boolean isEmpty() {
        return surrogate.isEmpty();
    }

    public boolean contains(Object o) {
        return surrogate.contains(o);
    }

    public Iterator <T> iterator() {
        return new ReadOnlyIterator<T>(surrogate.iterator());
    }

    public Object[] toArray() {
        return surrogate.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return surrogate.toArray(a);
    }

    public boolean add(T o) {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }

    public boolean remove(Object o) {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }

    public boolean containsAll(Collection c) {
        return surrogate.containsAll(c);
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }

    public void clear() {
        throw new UnsupportedException("You cannot modify a read-only collection.");
    }
}
