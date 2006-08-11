/*
 * Created on Jun 10, 2006
 * 
 * file: ILocalThreadManager.java
 */
package ddproto1.debugger.managing;

import ddproto1.debugger.managing.tracker.ILocalThread;

/**
 * Thread manager - interface for accessing the local threads in a 
 * node proxy.
 * 
 * @author giuliano
 *
 */
public interface ILocalThreadManager extends IThreadManager{
    /**
     * Returns a snapshot of the currently active threads. 
     * Note that the snapshot is not reliable - the list of
     * active threads is in continuous change.
     * 
     * @return
     */
    public ILocalThread [] getLocalThreads();
    
    /**
     * Returns a local thread with a specific global id.
     * 
     * @param uuid the ID of the local thread
     * @return the local thread with corresponding global ID, 
     * or <b>null</b> if no such thread exists.
     */
    public ILocalThread    getLocalThread(Integer uuid);
}
