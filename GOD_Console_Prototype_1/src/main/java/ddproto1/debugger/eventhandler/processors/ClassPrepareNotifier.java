/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DeferredEventExecutor.java
 */

package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;

import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdResolutionContextImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class ClassPrepareNotifier extends AbstractEventProcessor {

    private static final ProcessingContextManager pcm = ProcessingContextManager.getInstance();
    
    private DeferrableRequestQueue pending;
     
    public ClassPrepareNotifier(DeferrableRequestQueue pending){
        this.pending = pending;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventListener#process(com.sun.jdi.event.Event)
     */
    public void specializedProcess(Event e) {
        
        IProcessingContext ctx = pcm.getProcessingContext();
        
        ClassPrepareEvent cpe = (ClassPrepareEvent) e;
        
        /* This is the price we pay for being general - you must
         * configure a lot of stuff before doing something */
        StdPreconditionImpl ip = new StdPreconditionImpl();
        StdResolutionContextImpl rc = new StdResolutionContextImpl();
        ip.setClassId(cpe.referenceType().name());
        ip.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        rc.setContext(cpe.referenceType());
        rc.setPrecondition(ip);
        
        /* This is the really important part */
        pending.resolveForContext(rc);
        
        /* Votes for resuming events. */
        ctx.vote(IEventManager.RESUME_SET);
    }
}
