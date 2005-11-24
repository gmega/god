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
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.request.AbstractDeferrableRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.NoSuchElementError;
import ddproto1.exception.RequestProcessorException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.interfaces.IUICallback;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.JDIMiscTrait;
import ddproto1.util.traits.commons.ConversionTrait;


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
    
	private static ConversionTrait ct = ConversionTrait.getInstance();
	
    private static final String module = "ComponentBoundaryRecognizer -";
    
    private DistributedThreadManager dtm;
    private VirtualMachineManager parent;
    private IUICallback ui;
    private Set stublist;
    private ClassType taggerClass;
    private Map<Integer, Byte> pendingModifiers = new HashMap<Integer, Byte>();
    
    private static MessageHandler mh = MessageHandler.getInstance();
    private static TaggerProxy tagger = TaggerProxy.getInstance();
    
    private static ConversionTrait fh = ConversionTrait.getInstance();
    
    public ComponentBoundaryRecognizer(DistributedThreadManager dtm,
            Set stublist, IUICallback ui, VirtualMachineManager parent){
        this.dtm = dtm;
        this.stublist = stublist;
        this.ui = ui;
        this.parent = parent;
        
        /* Declare its interest in changing the vote for printing sources. */
        parent.getVotingManager().declareVoterFor(SourcePrinter.NO_SOURCE);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.EventProcessor#specializedProcess(com.sun.jdi.event.Event)
     *
     * REMARK I don't think this method has to be synchronized. 
     */
    public synchronized void specializedProcess(Event e) {
        StepEvent se = (StepEvent)e;
        Location l = se.location();

        /* There are basically two boundaries we might want to check. */        
        VirtualMachine vm = se.virtualMachine();
        ReferenceType klass = l.declaringType();
        ThreadReference te = se.thread();
        
        /* First one - we have stepped into remote object stub. */
        try {
            if (stublist.contains(klass.name())) {
                
                StepRequest request = (StepRequest)se.request();
                /* This step event is part of the thread stopping
                 * protocol. We shouldn't mess with it.
                 */
                if(isTrap(request))
                    return;

                /* REMARK DO NOT hotswap the tagger class. */
                if (taggerClass == null) {
                    taggerClass = tagger.getClass(vm,
                            DebuggerConstants.TAGGER_CLASS_NAME);
                }

                /*
                 * If we have stepped into a CORBA stub, then we must check if
                 * this thread is actually "stepping remote".
                 */
                IntegerValue dt_uuid = tagger.getRemoteUUID(te, taggerClass);

                /* This thread hasn't got a tag id */
                if (dt_uuid == null) {
                    mh.getWarningOutput().println(module 
                                    + " Warning: Untagged thread has stepped into "
                                    + " CORBA stub - debugger workings will be affected. "
                                    + "Check if that's what you really intended to do.");
                    return;
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
                byte mode = (request.depth() == StepRequest.STEP_INTO) ? DistributedThread.STEPPING
                        : DistributedThread.RUNNING;
                pendingModifiers.put(dt_uuid.intValue(), (byte)(mode | DistributedThread.ILLUSION));

                /* There should be no source printing in this processing chain.
                 * Otherwise we'll see part of the stub code (something we don't really want to).
                 */
                IProcessingContext ipc = ProcessingContextManager.getInstance().getProcessingContext();
                ipc.vote(SourcePrinter.NO_SOURCE);
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
    
    protected void applyPostUpcallModifiers(DistributedThread dt){
    	int dt_uuid = dt.getId();
    	Byte modifier = pendingModifiers.get(dt_uuid);
    	if(modifier == null) return;
    	else{
    		pendingModifiers.remove(dt_uuid);
    		try {
				dt.setStepping(modifier);
			} catch (InvalidAttributeValueException e) {
				mh.getErrorOutput().println(
						"Error setting deferred modifiers in thread "
								+ ct.uuid2Dotted(dt_uuid));
				mh.printStackTrace(e);
			}
    	}
    }
    
    private boolean isTrap(StepRequest se){
        if(se.getProperty(ClientSideThreadStopper.THREAD_TRAP_PROTOCOL) != null)
            return true;
        return false;
    }
}
