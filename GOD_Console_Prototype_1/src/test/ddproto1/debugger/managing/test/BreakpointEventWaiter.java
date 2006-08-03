/*
 * Created on 1/08/2006
 * 
 * file: BreakpointEventWaiter.java
 */
package ddproto1.debugger.managing.test;

import java.util.concurrent.CountDownLatch;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakpointEventWaiter implements IBreakpointListener{
    
    public static final int ADD = 0;
    public static final int REMOVE = 0;
    public static final int CHANGE = 0;
    
    private volatile int fMode;
    private volatile IBreakpoint fBkp;
    private volatile IMarkerDelta fDelta;
    
    private final CountDownLatch cdLatch = new CountDownLatch(1);
    
    public BreakpointEventWaiter(int mode, IBreakpoint bkp){
        synchronized(this){
            fBkp = bkp;
            fMode = mode;
            DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
        }
    }

    public synchronized void breakpointAdded(IBreakpoint breakpoint) {
        checkOccurrence(ADD, breakpoint, null);
    }

    public synchronized void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        checkOccurrence(REMOVE, breakpoint, delta);
    }

    public synchronized void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) { 
        checkOccurrence(CHANGE, breakpoint, delta);
    }
    
    private synchronized void checkOccurrence(int mode, IBreakpoint bkp, IMarkerDelta mDelta){
        if(fMode != mode) return;
        if(bkp == fBkp) eventOccurred(mDelta);
    }
    
    protected synchronized void eventOccurred(IMarkerDelta mDelta){
        fDelta = mDelta;
        cdLatch.countDown();
    }

    
    public IMarkerDelta getMarkerDelta(){
        return fDelta;
    }
    
    public void waitForEvent()
        throws InterruptedException{
        cdLatch.await();
    }
}
