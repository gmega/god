/*
 * Created on 1/08/2006
 * 
 * file: IDistributedThread.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;

public interface IDistributedThread extends IThread{

    /*******************************************************************************
     * These methods are called by local threads when they have events to report.  *
     *******************************************************************************/
    /**
     * Method called by local thread when it gets hit by a breakpoint.
     */
    public void hitByBreakpoint(IBreakpoint bp, ILocalThread lt);

    /**
     * Method called by local thread when it finishes stepping.
     */
    public void beganStepping(ILocalThread lt);

    /**
     * Method called by local thread when it begins stepping.
     */
    public void finishedStepping(ILocalThread lt);

    /**
     * Method called by local thread when it's suspended.
     */
    public void suspended(ILocalThread lt);

    /**
     * Method called by local thread when it's resumed.
     */
    public void resumed(ILocalThread lt);

    /**
     * Method called by local thread when it dies.
     */
    public void died(ILocalThread lt);
    
    public int getId();

}