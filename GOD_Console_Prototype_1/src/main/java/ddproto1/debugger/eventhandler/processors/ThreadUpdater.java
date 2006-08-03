/*
 * Created on Aug 17, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ThreadUpdater.java
 */

package ddproto1.debugger.eventhandler.processors;

import org.apache.log4j.Logger;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;

import ddproto1.debugger.managing.IJavaThreadManager;
import ddproto1.debugger.managing.ThreadManager;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class ThreadUpdater extends AbstractEventProcessor{
    
    private static final String module = "ThreadUpdater -";
    private static final Logger logger = MessageHandler.getInstance().getLogger(ThreadUpdater.class);
    
    public ThreadUpdater(){ }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.EventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    public void specializedProcess(Event e) {
        try{
            try{
            	ThreadReference tr = ThreadManager.getThreadFromEvent(e);
            	IJavaThreadManager tm = dc.getVMM().getThreadManager();
            	tm.setCurrentThread(tr);
            }catch(Exception ex){
            	// Logs an error but doesn't interrupt the processing chain.
            	logger.error("Could not update current thread. Subsequent failures may occur.", ex);
            }
            
        }catch(Exception ex){
            MessageHandler mh = MessageHandler.getInstance();
            mh.getErrorOutput().println(module + " Failed to update current thread.");
            mh.printStackTrace(ex);
        }
    }
}
