/*
 * Created on Sep 20, 2005
 * 
 * file: ReadOnlyIterable.java
 */
package ddproto1.util.collection.commons;

import java.util.Iterator;

public class ReadOnlyIterable<K> implements Iterable<K>{
    
    private Iterable<K> delegate;
    
    public ReadOnlyIterable(Iterable<K> delegate){
        this.delegate = delegate;
    }

    public Iterator<K> iterator() {
        return new ReadOnlyIterator<K>(delegate.iterator());
    }   
}
