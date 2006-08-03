/*
 * Created on 1/08/2006
 * 
 * file: DebugBreakpointWaiter.java
 */
package org.eclipse.jdt.debug.testplugin;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;

public class DebugBreakpointWaiter extends DebugEventWaiter {

    private volatile IBreakpoint fBkp;
    private volatile IThread fThread;
    
    public DebugBreakpointWaiter(int eventType, IBreakpoint bkp) {
        super(eventType);
        fBkp = bkp;
    }
    
    @Override
    public boolean accept(DebugEvent evt){
        Object src = evt.getSource();
        if(!(src instanceof IThread)) return false;
        
        IThread thread = (IThread)src;
        for(IBreakpoint bkp : thread.getBreakpoints())
            if(fBkp == bkp){
                fThread = thread;
                return true;
            }
        
        return false;
    }
    
    public IThread getSuspendedThread(){
        return fThread;
    }

}
