/*
 * Created on Aug 3, 2005
 * 
 * file: IGUIDManager.java
 */
package ddproto1.debugger.managing.distributed;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.ResourceLimitReachedException;

public interface IGUIDManager {
    public int  leaseGUID(Object o) throws ResourceLimitReachedException, DuplicateSymbolException;
    public void leaseGUID(Object o, int guid) throws DuplicateSymbolException, NoSuchSymbolException;
    public void releaseGUID(Object o);
}
