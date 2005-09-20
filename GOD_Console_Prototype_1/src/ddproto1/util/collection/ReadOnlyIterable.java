/*
 * Created on Sep 20, 2005
 * 
 * file: ReadOnlyIterable.java
 */
package ddproto1.util.collection;

import java.util.Iterator;

public class ReadOnlyIterable<K> implements Iterable<K>{
    
    private Iterable<K> delegate;
    
    public ReadOnlyIterable(Iterable<K> delegate){
        this.delegate = delegate;
    }

    public Iterator<K> iterator() {
        return new ReadOnlyIterator(delegate.iterator());
    }   
    
    private class ReadOnlyIterator implements Iterator<K>{
        
        private Iterator<K> itDelegate;
        
        public ReadOnlyIterator(Iterator<K> itDelegate){
            this.itDelegate = itDelegate;
        }

        public boolean hasNext() {
            return itDelegate.hasNext();
        }

        public K next() {
            return itDelegate.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("Removal is not supported by this iterator.");
        }
    }


}
