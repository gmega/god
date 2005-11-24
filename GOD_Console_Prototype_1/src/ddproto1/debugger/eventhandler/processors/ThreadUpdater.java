/*
 * Created on Aug 17, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ThreadUpdater.java
 */

package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;

import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.ThreadManager;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class ThreadUpdater extends AbstractEventProcessor{
    
    private static final String module = "ThreadUpdater -";
    
    public ThreadUpdater(){ }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.EventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    public void specializedProcess(Event e) {
        try{
            
            ThreadReference tr = ThreadManager.getThreadFromEvent(e);
            IVMThreadManager tm = dc.getVMM().getThreadManager();
            tm.setCurrentThread(tr);
            
        }catch(Exception ex){
            MessageHandler mh = MessageHandler.getInstance();
            mh.getErrorOutput().println(module + " Failed to update current thread.");
            mh.printStackTrace(ex);
        }
    }
}
