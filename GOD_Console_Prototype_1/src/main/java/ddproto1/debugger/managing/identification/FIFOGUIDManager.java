/*
 * Created on Feb 8, 2006
 * 
 * file: FIFOGUIDManager.java
 */
package ddproto1.debugger.managing.identification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.ResourceLimitReachedException;
import ddproto1.exception.commons.InvalidAttributeValueException;

/**
 * This class is thread-safe, but it has low concurrency. We expect mostly
 * uncontended synchronization to happen here, though.
 * 
 * @author giuliano
 *
 */
public class FIFOGUIDManager implements IGUIDManager{
    
    private int minimumLength;
    private int maximumValue;

    /** It might sound silly to index integers by themselves,
     * but we're interested in supporting fast lookup through
     * a linked list. We could use an array that indexes list
     * nodes, but that would not only require that we built
     * our own linked list implementation but would probably
     * be more wasteful than using a hash table, which expands
     * and shrinks on demand.
     * 
     * Note: What we'd really need here is a LinkedSet. We want
     * to be able to test for inclusion really fast while being able
     * to maintain a queue by insertion order and remove elements
     * from the linked list in almost constant time.
     */
    private final LinkedMap id2id = new LinkedMap();
    
    /**
     * The lease table keeps track of which IDs were leased
     * to which objects. 
     */
    private final Map<Object, Integer> leaseTable = new HashMap<Object, Integer>();
    
    
    /**
     * Creates a new FIFOGUIDManager. Leased IDs will go from 
     * 0 to possibleIDValues - reuseConstraint. The FIFOGUIDManager
     * ensures that there are always at least reuseConstraint free IDs. 
     * The IDs are served in a FIFO order, where released IDs go to the
     * end of the queue. Initially, the IDs are distributed sequentially.
     * 
     * 
     * @param maxIDvalue
     * @param reuseConstraint
     * @throws InvalidAttributeValueException
     */
    public FIFOGUIDManager(int possibleIDValues, int reuseConstraint)
        throws InvalidAttributeValueException
    {
        if(possibleIDValues <= reuseConstraint) throw new InvalidAttributeValueException(
                "Constrained value number cannot be greater than the number of possible values.");
        
        synchronized(this){
            minimumLength = reuseConstraint;
            maximumValue = possibleIDValues - 1;
            populateQueue(maximumValue);
        }
    }

    public synchronized int leaseGUID(Object o) throws ResourceLimitReachedException, DuplicateSymbolException {
        checkLeased(o);
        if(id2id.size() <= minimumLength) 
        	throw new ResourceLimitReachedException("ID pool is depleted.");
        
        int nextId = (Integer)id2id.firstKey(); // LinkedMap respects insertion order.
        doLease(o, nextId);
        return nextId;
    }

    public synchronized void leaseGUID(Object o, int guid) throws DuplicateSymbolException, NoSuchSymbolException {
        checkLeased(o);
        if(guid > maximumValue)
        	throw new NoSuchSymbolException("Invalid GUID.");
        else if(!id2id.containsKey(guid)) // In use?
            throw new DuplicateSymbolException("GUID already in use.");
        else if(id2id.indexOf(guid) >= (id2id.size() - minimumLength))
        	throw new NoSuchSymbolException("GUID is not currently available.");
        	
        doLease(o, guid);
    }
    
    private void doLease(Object o, Integer guid){
        leaseTable.put(o, guid);
        id2id.remove((Object)guid);
    }

    public synchronized void releaseGUID(Object o) {
        if(!leaseTable.containsKey(o)) return;
        
        Integer leasedId = leaseTable.get(o);
        assert !id2id.containsKey(leasedId);
        id2id.put(leasedId, leasedId);
        leaseTable.remove(o);
    }
    
    private void populateQueue(int maxID){
        for(int i = 0; i <= maxID; i++)
            id2id.put(i, i);
    }
    
    private synchronized void checkLeased(Object o) throws DuplicateSymbolException{
        if(leaseTable.containsKey(o)) throw new DuplicateSymbolException("You cannot assign two GUIDs to one object.");
    }

    public synchronized Integer currentlyLeasedGUID(Object o) {
        return(leaseTable.get(o));
    }
}
