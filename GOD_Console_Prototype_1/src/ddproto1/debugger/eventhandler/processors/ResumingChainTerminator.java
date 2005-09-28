/*
 * Created on Jan 11, 2005
 * 
 * file: ResumingChainTerminator.java
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;

import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.ProcessingContextManager;

/**
 * @author giuliano
 *
 */
public class ResumingChainTerminator extends AbstractEventProcessor {

    private static final ProcessingContextManager pcm = ProcessingContextManager.getInstance();
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.BasicEventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    protected void specializedProcess(Event e) {
        pcm.getProcessingContext().vote(IEventManager.RESUME_SET);  
    }

}
