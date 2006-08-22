/*
 * Created on 15/08/2006
 * 
 * file: BreakpointCreationWaiter.java
 */
package ddproto1.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakpointCreationWaiter implements IBreakpointListener{

    private final CountDownLatch cdLatch = new CountDownLatch(1);
    private volatile IBreakpoint fBkp;
    
    public BreakpointCreationWaiter(IBreakpoint bkp){
        fBkp = bkp;
        if(DebugPlugin.getDefault().getBreakpointManager().isRegistered(fBkp))
            cdLatch.countDown();
        else
            DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
    }
    
    public void breakpointAdded(IBreakpoint breakpoint) {
        if(fBkp == breakpoint){
            DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
            cdLatch.countDown();
        }
    }
    
    public IBreakpoint getBreakpoint(){
        return fBkp;
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    }
    
    public boolean awaitCreation(int timeout){
        try{
            return cdLatch.await(timeout, TimeUnit.MILLISECONDS);
        }catch(InterruptedException ex){
            return false;
        }
    }

}
