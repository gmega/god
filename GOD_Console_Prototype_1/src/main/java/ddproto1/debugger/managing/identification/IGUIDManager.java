/*
 * Created on Aug 3, 2005
 * 
 * file: IGUIDManager.java
 */
package ddproto1.debugger.managing.identification;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.ResourceLimitReachedException;

/**
 * This interface represents a Globally Unique IDentifier (GUID) management interface.
 * It allows clients to lease available GUIDs, lease specific GUIDs, release GUIDs
 * and query for leased GUIDs.
 * 
 * @author giuliano
 *
 */
public interface IGUIDManager {
    /**
     * Leases an avai GUID isn't currently avaliable.lable GUID to the object passed as parameter.
     * 
     * @param o The object that'll be associated with this GUID.
     * @return An automatically assigned GUID.
     * @throws ResourceLimitReachedException If no GUID is available.
     * @throws DuplicateSymbolException If this Object has already leased
     * one GUID.
     */
    public int     leaseGUID(Object o) throws ResourceLimitReachedException, DuplicateSymbolException;
    
    /**
     * Attempts to lease a specific GUID to an object.
     * 
     * @param o The object that'll be associated with this GUID. 
     * @param guid The desired GUID.
     * @throws DuplicateSymbolException If <b>o</b> is already bound to a GUID.
     * @throws NoSuchSymbolException If GUID <b>guid</b> isn't currently available.
     */
    public void    leaseGUID(Object o, int guid) throws DuplicateSymbolException, NoSuchSymbolException;
    
    /**
     * Releases a previously leased GUID. If the object passed
     * on as parameter hasn't been bound to any GUID, this operation
     * does nothing.
     * 
     * @param o
     */
    public void    releaseGUID(Object o);
    
    /**
     * Queries what the currently leased GUID for Object <b>o</b> is.
     * 
     * @param o The object to query.
     * @return The currently leased GUID, or <b>null</b> if there's no GUID.
     */
    public Integer currentlyLeasedGUID(Object o);
}
