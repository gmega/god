/*
 * Created on Sep 27, 2004
 * 
 * file: ComponentBoundaryRecognizer.java
 */
package ddproto1.debugger.managing.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.jdi.ClassType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.eventhandler.processors.BasicEventProcessor;
import ddproto1.debugger.eventhandler.processors.SourcePrinter;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.NoSuchElementError;
import ddproto1.exception.RequestProcessorException;
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
public class ComponentBoundaryRecognizer extends BasicEventProcessor{
    
    private static final String module = "ComponentBoundaryRecognizer -";
    
    private DistributedThreadManager dtm;
    private VirtualMachineManager parent;
    private IUICallback ui;
    private Set stublist;
    private ClassType taggerClass;
    
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
    protected synchronized void specializedProcess(Event e) {
        StepEvent se = (StepEvent)e;
        Location l = se.location();

        /* There are basically two boundaries we might want to check. */        
        VirtualMachine vm = se.virtualMachine();
        ReferenceType klass = l.declaringType();
        ThreadReference te = se.thread();
        
        /* First one - we have stepped into remote object stub. */
        try {
            if (stublist.contains(klass.name())) {

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

                /* Remote mode is disabled. Nothing to do. */
                if (!ui.queryIsRemoteOn(dt_uuid.intValue())) {
                    return;
                }

                /* Otherwise, marks the distributed thread as stepping. */
                try{
                    DistributedThread dt = dtm.getByUUID(dt_uuid.intValue());
                    dt.setStepping(true);
                }catch(NoSuchElementError ex){
                    /* The thread hasn't yet been promoted. Creates a deferrable request to change the thread status. */
                    ChangeStatusRequest csr = new ChangeStatusRequest(fh.int2Hex(dt_uuid.intValue()), true);
                    parent.getDeferrableRequestQueue().addEagerlyResolve(csr);
                }
                    

                /* Creates a step return request so we can retake from where we were. */
                // REMARK I must find out why the heck I have to do this every time I need to create a new step request.
                // FIXME This step request should vanish away. It can't handle server-side breakpoints.
//                jdim.clearPreviousStepRequests(te);
//                StepRequest old = (StepRequest)e.request();
//                StepRequest sr = vm.eventRequestManager().createStepRequest(te, old.size(), StepRequest.STEP_OUT);
//                sr.setSuspendPolicy(pm.getPolicy("request.step"));
//                sr.putProperty(DebuggerConstants.VMM_KEY, e.request().getProperty(DebuggerConstants.VMM_KEY));
//                sr.addCountFilter(1);
//                sr.enable();

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
    
    private class ChangeStatusRequest implements IDeferrableRequest{

        private String dt_hex_id;
        private boolean toWhich;
        private List requirements;
        
        private ChangeStatusRequest(String dt_hex_id, boolean toWhich){
            this.toWhich = toWhich;
            this.dt_hex_id = dt_hex_id;
            
            /* Resolution preconditions */
            StdPreconditionImpl spi = new StdPreconditionImpl();
            spi.setClassId(dt_hex_id);
            spi.setType(new StdTypeImpl(IDeferrableRequest.THREAD_PROMOTION, IDeferrableRequest.MATCH_ONCE));
            this.requirements = new ArrayList();
            requirements.add(spi);
        }
        
        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
         */
        public Object resolveNow(IResolutionContext context) throws Exception {
            if(!context.getPrecondition().getClassId().equals(dt_hex_id))
                throw new InternalError("Invalid precondition!");
            
            DistributedThreadManager dtm = (DistributedThreadManager)context.getContext();

            DistributedThread dt;
            try{
                /* The protocol is: if the event cannot be resolved, return null.
                 */
                dt = dtm.getByUUID(fh.hex2Int(dt_hex_id));
                dt.setStepping(toWhich);
            }catch(NoSuchElementError err){
                return null;
            }
            
            return dt;
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#getRequirements()
         */
        public List getRequirements() {
            return requirements;
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#addResolutionListener(ddproto1.debugger.request.IResolutionListener)
         */
        public void addResolutionListener(IResolutionListener listener) {
            
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#removeResolutionListener(ddproto1.debugger.request.IResolutionListener)
         */
        public void removeResolutionListener(IResolutionListener listener) {

        }
    }
}
