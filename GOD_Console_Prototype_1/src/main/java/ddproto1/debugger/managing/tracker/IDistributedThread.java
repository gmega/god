/*
 * Created on 1/08/2006
 * 
 * file: IDistributedThread.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;

/**
 * This interface specifies the interface of a distributed thread.
 * It mostly contains messages that must be sent from a local thread 
 * (implements ILocalThread) to its parent distributed thread (implements 
 * this interface).
 * 
 * @author giuliano
 * @see ddproto1.debugger.managing.tracker.ILocalThread
 */
public interface IDistributedThread extends IThread{
    /**
     * Reports that a breakpoint has been hit.
     * 
     * @param bp reference to the breakpoint that has been hit.
     * @param lt reference to the local thread that's been hit by
     * the breakpoint.
     */
    public void hitByBreakpoint(IBreakpoint bp, ILocalThread lt);

    /**
     * Reports that a local thread has switched from running to 
     * suspended state (when this method is called, the switch
     * has already been performed).
     * 
     * @param lt reference to the local thread that's been suspended.
     * @param detail details the reason for suspension (see DebugEvent).
     *  
     * @see org.eclipse.debug.core.DebugEvent
     */
    public void suspended(ILocalThread lt, int detail);

    /**
     * Reports that a local thread has switched from suspended to 
     * running state (when this method is called, the switch
     * has already been performed).
     * 
     * @param lt reference to the local thread that's been resumed.
     * @param detail details the reason for resumption (see DebugEvent).
     * the breakpoint.
     */
    public void resumed(ILocalThread lt, int detail);

    /**
     * Reports that a give local thread has died. This may happen
     * because the thread has terminated, but it may also happen
     * because the machine to which this local thread belongs has
     * been reported dead. 
     * 
     * @param lt reference to the local thread that has died.
     */
    public void died(ILocalThread lt);
    
    /**
     * Returns the globally unique ID for this distributed thread, 
     * which is equal to the globally unique ID of its base local
     * thread.
     * 
     * @return the globally unique ID of this distributed thread.
     */
    public int getId();
}