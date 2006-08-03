/*
 * Created on Mar 31, 2005
 * 
 * file: ClientSideThreadStopper.java
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.util.JDIMiscUtil;
import ddproto1.util.PolicyManager;

/**
 * This class is responsible for implementing the thread resume protocol that correctly puts
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

    private static final JDIMiscUtil jmt = JDIMiscUtil.getInstance();
    private static final PolicyManager pm = PolicyManager.getInstance();
    private static final Integer protocolSlot = new Integer(5);
    private static IVMManagerFactory vmmf = VMManagerFactory.getInstance();
    
    public void specializedProcess(Event e) {
        if(e instanceof BreakpointEvent) processPhaseOne((BreakpointEvent)e);
        else if(e instanceof StepEvent) processPhaseTwo((StepEvent)e);
        else
            throw new UnsupportedException(ClientSideThreadStopper.class
                    .toString() 
                    + " Does not support event type " + e.getClass().toString());
    }
    
        
    private void processPhaseOne(BreakpointEvent bpe){
        Method method = bpe.location().method();
        String fullMethodName = method.declaringType().name() + "." + method.name();
        
        if(fullMethodName.equals(DebuggerConstants.THREAD_TRAP_METHOD_NAME)){
            String vmid = (String)bpe.request().getProperty(DebuggerConstants.VMM_KEY);
            ThreadReference tr = bpe.thread();
            
            StepRequest sr = this.stepAndResume(tr, vmid, true);
            sr.putProperty(protocolSlot, 2);
            sr.putProperty(THREAD_TRAP_PROTOCOL, 0);
        }
    }
    
    private void processPhaseTwo(StepEvent se){
        Integer phase = (Integer)se.request().getProperty(protocolSlot);
        if(phase == null) return;
        else if ((phase == 2) || (phase == 3)){
            StepRequest sr = this.stepAndResume(se.thread(), (String) se.request().getProperty(
                    DebuggerConstants.VMM_KEY), true);
            sr.putProperty(protocolSlot, phase+1);
            sr.putProperty(THREAD_TRAP_PROTOCOL, 0);
        } else if (phase == 4){
            return;
        }else{
            throw new UnsupportedException(
                    "Unsupported protocol or protocol error.");
        }
    }
    
    private StepRequest stepAndResume(ThreadReference tr, String vmid, boolean clear){
        IJavaNodeManager vmm = (IJavaNodeManager)vmmf.getNodeManager(vmid).getAdapter(IJavaNodeManager.class);
        EventRequestManager erm = vmm.virtualMachine().eventRequestManager();
        
        if(clear) jmt.clearPreviousStepRequests(tr, vmm);
        
        StepRequest sr = erm.createStepRequest(tr, StepRequest.STEP_MIN, StepRequest.STEP_OUT);
        sr.putProperty(DebuggerConstants.VMM_KEY, vmid);
        sr.setSuspendPolicy(pm.getPolicy("request.step"));
        sr.addCountFilter(1);
        sr.enable();
        
        /* Votes for resuming the set. */
        IProcessingContext ipc = ProcessingContextManager.getInstance().getProcessingContext();
        ipc.vote(IEventManager.RESUME_SET);
        
        return sr;
    }

}
