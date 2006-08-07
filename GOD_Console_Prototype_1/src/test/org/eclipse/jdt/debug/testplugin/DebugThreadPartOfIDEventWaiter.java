/*
 * Created on 5/08/2006
 * 
 * file: DebugBreakpointLTDTWaiter.java
 */
package org.eclipse.jdt.debug.testplugin;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IBreakpoint;

import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;

public class DebugThreadPartOfIDEventWaiter extends DebugEventWaiter {

    private volatile int fDistributedID;
    
    public DebugThreadPartOfIDEventWaiter(int eventType, int distributedID) {
        super(eventType);
        fDistributedID = distributedID;
    }
    
    public boolean accept(DebugEvent evt){
        if(!(evt.getSource() instanceof ILocalThread)) return false;
        
        ILocalThread lThread = (ILocalThread)evt.getSource();
        
        IDistributedThread dThread = lThread.getParentDistributedThread();
        if(dThread instanceof NilDistributedThread) return false;
        if(dThread.getId() == fDistributedID){
            return true & super.accept(evt);
        }
        return false;
    }

}
