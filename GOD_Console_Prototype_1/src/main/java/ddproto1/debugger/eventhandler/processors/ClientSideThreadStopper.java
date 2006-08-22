/*
 * Created on Mar 31, 2005
 * 
 * file: ClientSideThreadStopper.java
 */
package ddproto1.debugger.eventhandler.processors;

import org.eclipse.debug.core.DebugException;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.StepRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.JavaThread;
import ddproto1.debugger.managing.StepRequestSpec;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.exception.commons.UnsupportedException;

/**
 * This class is responsible for implementing the thread resumption protocol that correctly puts
 * the client-side thread at the place it's meant to be. It basically discards a breakpoint 
 * and sets two step requests, one of which should be discarded. 
 * 
 * Can be flyweighted as it strives not to keep any internal, node-specific state. 
 * Does not need synchronization. Extrinsic state comes with the request.
 * 
 * 
 * @author giuliano
 *
 */
public class ClientSideThreadStopper extends AbstractEventProcessor{
    
    public static final Object THREAD_TRAP_PROTOCOL = new Object();
    
    private static final Object PROTOCOL_SLOT = new Object();
    private static final IVMManagerFactory vmmf = VMManagerFactory.getInstance();
    
    public void specializedProcess(Event e) 
        throws DebugException
    {
        if(e instanceof BreakpointEvent) processPhaseOne((BreakpointEvent)e);
        else if(e instanceof StepEvent) processOtherPhases((StepEvent)e);
        else
            throw new UnsupportedException(ClientSideThreadStopper.class
                    .toString() 
                    + " Does not support event type " + e.getClass().toString());
    }
    
        
    private void processPhaseOne(BreakpointEvent bpe)
        throws DebugException
    {
        Method method = bpe.location().method();
        String fullMethodName = method.declaringType().name() + "." + method.name();
        
        if(fullMethodName.equals(DebuggerConstants.THREAD_TRAP_METHOD_NAME)){
            advancePhase(bpe, 1, false);
        }
    }
    
    private void processOtherPhases(StepEvent se)
        throws DebugException
    {
        Integer phase = (Integer)se.request().getProperty(PROTOCOL_SLOT);
        if(phase == null) return;
        else if ((phase == 2) || (phase == 3)){
            advancePhase(se, phase, false);
        } else if (phase == 4){
            Location loc = se.location();
            int line = loc.lineNumber();
            if(line == DebuggerConstants.MAX_SOURCE_LINE){
                advancePhase(se, phase, true);
            }else{
                getJavaThread(se).prepareForRemoteStepReturn();
            }
                
        }else if (phase == 5){
            return;
        }else{
            throw new UnsupportedException(
                    "Unsupported protocol or protocol error.");
        }
    }
    
    private void advancePhase(LocatableEvent se, int currentPhase, boolean lastPhase)
        throws DebugException
    {
        JavaThread jThread = getJavaThread(se);
        
        /** Generates events only if at last phase. Otherwise, 
         * step request should be quiet (we don't want noise from
         * our internal protocols reaching the user interface).
         */
        StepRequestSpec srSpec = 
            (lastPhase)?
            new StepRequestSpec(
                StepRequestSpec.GENERATE_STEP_END |
                StepRequestSpec.PARENT_GENERATE_STEP_END)
                :StepRequestSpec.quietDelayed();
        
        srSpec.setProperty(DebuggerConstants.VMM_KEY, jThread.getDebugTarget().getName());
        srSpec.setProperty(PROTOCOL_SLOT, currentPhase+1);
        srSpec.setProperty(THREAD_TRAP_PROTOCOL, 0);

        // Clears any pending step requests before attempting to place a new one.
        jThread.clearPendingStepRequests();
        jThread.step(StepRequest.STEP_OUT, srSpec);
        
        /* Votes for resuming the set. */
        IProcessingContext ipc = ProcessingContextManager.getInstance().getProcessingContext();
        ipc.vote(IEventManager.RESUME_SET);
    }
    
    private JavaThread getJavaThread(LocatableEvent le){
        IJavaNodeManager nManager = (IJavaNodeManager) vmmf.getNodeManager(
                vmmf.getGidByVM(le.virtualMachine())).getAdapter(
                IJavaNodeManager.class);
        return nManager.getThreadManager().getAdapterForThread(le.thread());
    }

}
