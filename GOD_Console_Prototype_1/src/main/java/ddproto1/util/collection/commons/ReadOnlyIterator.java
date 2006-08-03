/*
 * Created on Apr 24, 2005
 * 
 * file: ReadOnlyIterator.java
 */
package ddproto1.util.collection.commons;

import java.util.Iterator;

import ddproto1.exception.commons.UnsupportedException;

public class ReadOnlyIterator <E> implements Iterator <E>{

    private Iterator <E> iterator;
    
    public ReadOnlyIterator(Iterator <E> surrogate){
        this.iterator = surrogate;
    }
    
    public boolean hasNext() {
        return iterator.hasNext();
    }

    public E next() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedException("You cannot remove elements from a read-only collection.");
    }
}
