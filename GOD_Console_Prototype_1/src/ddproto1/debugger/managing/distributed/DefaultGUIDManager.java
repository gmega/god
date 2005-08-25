/*
 * Created on Aug 3, 2005
 * 
 * file: DefaultGUIDManager.java
 */
package ddproto1.debugger.managing.distributed;

import java.util.HashMap;
import java.util.Map;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.ResourceLimitReachedException;

public class DefaultGUIDManager implements IGUIDManager{
    
    private boolean [] guid_array;
    private Map<Object, Integer> lease_table = new HashMap<Object, Integer>();
    
    public DefaultGUIDManager(int maxGUIDs){
        guid_array = new boolean[maxGUIDs];
        for(int i = 0; i < maxGUIDs; i++) guid_array[i] = false;
    }

    /**
     * Leasing is O(n).
     */
    public int leaseGUID(Object o) throws ResourceLimitReachedException, DuplicateSymbolException {
        if(lease_table.containsKey(o)) throw new DuplicateSymbolException("You cannot assign two GUIDs to one object.");
        
        int k;
        for(k = 0; k < guid_array.length; k++){
           if(guid_array[k] == false){
               lease_table.put(o, k);
               guid_array[k] = true;
               break;
           }
        }
        
        if (k == guid_array.length)
            throw new ResourceLimitReachedException(
                    "Cannot lease - GUID pool is depleted.");
        
        return k;
    }

    public void leaseGUID(Object o, int guid) throws DuplicateSymbolException, NoSuchSymbolException {
        if (guid >= guid_array.length)
            throw new NoSuchSymbolException("GUID " + guid + " is not valid.");
        if (guid_array[guid] == true)
            throw new DuplicateSymbolException("GUID already in use");
        if(lease_table.containsKey(o)) 
            throw new DuplicateSymbolException("You cannot assign two GUIDs to one object.");

        lease_table.put(o, guid);
        guid_array[guid] = true;
        
    }

    /**
     * Releasing is O(1).
     */
    public void releaseGUID(Object o) {
        if(!lease_table.containsKey(o)) return;
        guid_array[lease_table.remove(o)] = false;
    }

}
