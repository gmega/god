/*
 * Created on Feb 1, 2005
 * 
 * file: LockingHashMap.java
 */
package ddproto1.util.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lockable hash map. Clients may call lockForReading or lockForWriting and perform
 * multiple operations while excluding other clients.
 * 
 * @author giuliano 
 * 
 * Note: This was quick and dirty. A decent implementation of a locking hash map
 *       with entry-level and table-level locking is overdue.
 *
 */
public class LockingHashMap <Key, Val> extends ConcurrentHashMap <Key, Val>{
    
    /**
     * 
     */
    private static final long serialVersionUID = 4050759412157069108L;
    private ReentrantReadWriteLock lock;
    private ThreadLocal <Thread> readers = new ThreadLocal<Thread>();
    private Lock readLock;
    private Lock writeLock;

    public LockingHashMap(boolean fair){
        super();
        lock = new ReentrantReadWriteLock(fair);
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    
    public void lockForReading(){
        readLock.lock();
        readers.set(Thread.currentThread());
    }
    
    public void unlockForReading(){
        readLock.unlock();
        readers.set(null);
    }
 
    public void lockForWriting(){
        writeLock.lock();
    }
    
    public void unlockForWriting(){
        writeLock.unlock();
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        prepareWrite();
        super.clear();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() 
    	throws CloneNotSupportedException
    {
        prepareWrite();
        Object ret = super.clone();
        return ret;
    }
    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        checkAcquired();
        return super.containsKey(key);
    }
    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        checkAcquired();
        return super.containsValue(value);
    }
    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set <Map.Entry<Key, Val>> entrySet() {
        checkAcquired();
        return super.entrySet();
    }
    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Val get(Object key) {
        checkAcquired();
        return super.get(key);
    }
    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        checkAcquired();
        return super.isEmpty();
    }
    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set <Key> keySet() {
        checkAcquired();
        return super.keySet();
    }
    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Val put(Key arg0, Val arg1) {
        prepareWrite();
        return super.put(arg0, arg1);
    }
    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends Key, ? extends Val> arg0) {
        prepareWrite();
        super.putAll(arg0);
    }
    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Val remove(Object key) {
        prepareWrite();
        return super.remove(key);
    }
    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        checkAcquired();
        return super.size();
    }
    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection <Val> values() {
        checkAcquired();
        return super.values();
    }
    
    private void prepareWrite(){
        checkAcquired();
        lock.writeLock();
    }
    
    
    private void checkAcquired(){
        /* This thread is either a reader or a writer. 
         * There's also an implicit semantics saying that
         * writers can read as much as they want. */
        if(lock.isWriteLockedByCurrentThread() ||
                (readers.get() != null)) return;
        
        throw new IllegalStateException("You cannot execute an operation " +
        		"before acquiring a lock.");
    }
}
