/*
 * Created on Sep 27, 2004
 * 
 * file: ComponentBoundaryRecognizer.java
 */
package ddproto1.debugger.managing.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;

import com.sun.jdi.ClassType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.StepRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.eventhandler.processors.ClientSideThreadStopper;
import ddproto1.debugger.eventhandler.processors.SourcePrinter;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IJavaThreadManager;
import ddproto1.debugger.managing.JavaThread;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.request.AbstractDeferrableRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.NoContextException;
import ddproto1.exception.RequestProcessorException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.interfaces.IUICallback;
import ddproto1.util.JDIMiscUtil;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.commons.ConversionUtil;


/**
 * This processor handles step event related decisions. Basically, it is responsible
 * for resuming threads when they hit stubs and when they return from servants. Though
 * I'm using a <b>CORBA</b> terminology here, this component should remain unchanged
 * if we switched middleware implementations for RMI, for instance.
 * 
 * REMARK There should be one ComponentBoundaryRecognizer for each node in the 
 * central agent.
 * 
 * @author giuliano
 *
 */
public class ComponentBoundaryRecognizer extends AbstractEventProcessor{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(ComponentBoundaryRecognizer.class);
    
    private IJavaNodeManager parent;
    private Set stublist;
    
    public ComponentBoundaryRecognizer(Set stublist, IJavaNodeManager parent){
        this.stublist = stublist;
        this.parent = parent;
        
        /* Declare its interest in changing the vote for printing sources. */
        parent.getVotingManager().declareVoterFor(IEventManager.NO_SOURCE);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.EventProcessor#specializedProcess(com.sun.jdi.event.Event)
     *
     * REMARK I don't think this method has to be synchronized. 
     */
    public synchronized void specializedProcess(Event e) {
        
        /* Checks if we stepped into a remote object stub. */
        StepEvent se = (StepEvent)e;
        Location l = se.location();
        ReferenceType klass = l.declaringType();
        ThreadReference tr = se.thread();
        
        try {
            if (stublist.contains(klass.name())) {
                
                StepRequest request = (StepRequest)se.request();
                                
                /* This step event is part of the thread stopping
                 * protocol. We shouldn't mess with it.
                 */
                if(isTrap(request)){
                    logger.debug("Trap request detected.");
                    return;
                }

                JavaThread javaLocalThread = (JavaThread) getJavaThreadManager()
                        .getAdapterForThread(tr).getAdapter(JavaThread.class);
                
                if(logger.isDebugEnabled()){
                    ConversionUtil cUtil = ConversionUtil.getInstance();
                    logger.debug("Will resume java local thread ("
                            + cUtil.uuid2Dotted(javaLocalThread.getGUID())
                            + ")\nwho belongs to machine <"
                            + javaLocalThread.getDebugTarget().getName()
                            + ">\nand has hash code " + +javaLocalThread.hashCode());
                }

                /* Remote mode is disabled. Nothing to do. 
                 * 
                 * if (!ui.queryIsRemoteOn(dt_uuid.intValue())) {
                 *     return;
                 * }
                 * FIX: That's the old code. Nowadays, if remote mode is disabled, we should 
                 * not OR the STEPPING_REMOTE modifier into the thread state. I currently 
                 * don't support disabling remote mode, though. 
                 * 
                 */ 

                /* Otherwise, marks the distributed thread as stepping remote. */
                javaLocalThread.toggleSuspendedByRemoteStepping();                

                /* Votes for hiding this step event from the user interface and for resuming
                 * the current event set.
                 */
                IProcessingContext ipc = ProcessingContextManager.getInstance().getProcessingContext();
                ipc.vote(IEventManager.NO_SOURCE);
                ipc.vote(IEventManager.RESUME_SET);
            }

            /* Second one - we have stepped out of a skeleton/servant */
            // This boundary check has been transferred to client code. Bit inconsistent.
                       
        } catch (Exception ex) {
            throw new RequestProcessorException(
                    "Error while processing request. Maybe tagger class hasn't been loaded?",
                    ex);
        }
    }
    
    private IJavaThreadManager getJavaThreadManager(){
        return (IJavaThreadManager)parent.getThreadManager().getAdapter(IJavaThreadManager.class);
    }
    
    private boolean isTrap(StepRequest se){
        if(se.getProperty(ClientSideThreadStopper.THREAD_TRAP_PROTOCOL) != null)
            return true;
        return false;
    }
}
