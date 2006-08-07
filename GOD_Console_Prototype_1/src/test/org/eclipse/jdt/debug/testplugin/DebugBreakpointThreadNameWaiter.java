/*
 * Created on 4/08/2006
 * 
 * file: DebugBreakpointThreadNameWaiter.java
 */
package org.eclipse.jdt.debug.testplugin;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;

import ddproto1.util.MessageHandler;

public class DebugBreakpointThreadNameWaiter extends DebugBreakpointWaiter {
    
    private static Logger logger = MessageHandler.getInstance().getLogger(
            DebugBreakpointThreadNameWaiter.class);

    private volatile String fThreadName;
    
    public DebugBreakpointThreadNameWaiter(int evtType, IBreakpoint bkp, String threadName){
        super(evtType, bkp);
        fThreadName = threadName;
    }
    
    public boolean accept(DebugEvent evt){
        Object src = evt.getSource();
        if(!super.accept(evt)) return false;
        
        try{
            IThread thread = (IThread)src;
            if(thread.getName().equals(fThreadName))
                return true;
        }catch(Throwable t){
            logger.error("Error while attempting to extract thread name." +
                    "Expected event might be missed.", t);
        }
        return false;
    }
    
}
