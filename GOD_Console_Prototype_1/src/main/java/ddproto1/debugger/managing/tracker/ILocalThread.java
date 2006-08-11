/*
 * Created on 16/05/2006
 * 
 * file: ILocalThread.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.model.IThread;
/**
 * Language-independent interface that must be implemented by all 
 * threads that wish to participate in a distributed thread.
 * 
 * @author giuliano
 *
 */
public interface ILocalThread extends IThread {
    /** 
     * Tells this local thread that it is now part of distributed 
     * thread 'dt'. It should notify 'dt' of all relevant events.
     * 
     * It also should return true if it was suspended when it got 
     * added to the distributed thread. It should be guaranteed that
     * Events that happen-after the suspension reported by returning 
     * true in this method should be reported through the notification 
     * methods of DistributedThread.  
     */ 
    public boolean setParentDT(IDistributedThread dt);
    
    /**
     * Retrieves and clears the "resumed by stepping remote" status of this local thread.
     * <br>
     * This method is called by the distributed thread manager after each client upcall
     * (although it could be called at server receive) so the manager may know whether
     * to stop the distributed thread when it reaches the server-side or not.
     * 
     * @param thread The local thread which to check and clear the "resumed by stepping
     * remote" status.
     * @return This method may return:
     * <ol>
     * <li><b>true</b> if this thread has been resumed because it has stepped into a remote 
     * object stub.</li>
     * <li><b>false</b> if this thread isn't remote stepping or the status has been previously 
     * cleared by a call to resumedBySteppingRemote.</li>
     * </ol>
     */
    public boolean resumedByRemoteStepping();
    
    /**
     * Tells this local thread that it is no longer part of distributed
     * thread 'dt'. After this method returns, 'dt' should not receive
     * any event notifications.
     * 
     * @param dt 
     */
    public void unbindFromParentDT();
    
    /** 
     * Returns the distributed thread in which this local thread is
     * currently participating, or a NilDistributedThread if none.
     * 
     * @return
     */
    public IDistributedThread getParentDistributedThread();
    
    /**
     * Clears all pending step requests from this thread.
     */
    public void clearPendingStepRequests();
    
    /**
     * Asks if the current thread has a pending step request (a step
     * request that has been placed but not yet been satisfied).
     * 
     * @return <b>true</b> if there is a pending step request, <b>false</b>
     *         otherwise.
     */
    public boolean hasPendingStepRequests();
    
    /**
     * Returns the Globally Unique ID for this local thread, or null
     * if this thread hasn't received an ID yet. 
     * 
     * @return
     */
    public Integer getGUID();
}
