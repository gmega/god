/*
 * Created on 16/05/2006
 * 
 * file: IDistributedThread.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;

public interface IResumeSuspendEventListener extends IThread
{
    public void hitByBreakpoint(IBreakpoint bp, ILocalThread lt);
    
    public void beganStepping(ILocalThread lt);
    public void finishedStepping(ILocalThread lt);
    
    public void suspended(ILocalThread lt);
    public void resumed(ILocalThread lt);
    
    public void died(ILocalThread lt);
}
