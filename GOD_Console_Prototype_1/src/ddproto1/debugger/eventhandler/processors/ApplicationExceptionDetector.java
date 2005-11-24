/*
 * Created on Sep 27, 2005
 * 
 * file: ApplicationExceptionDetector.java
 */
package ddproto1.debugger.eventhandler.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ExceptionEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.IVotingManager;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.managing.IDebugContext;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.managing.tracker.VirtualStackframe;
import ddproto1.debugger.managing.tracker.DistributedThread.VirtualStack;
import ddproto1.exception.PropertyViolation;
import ddproto1.exception.commons.UnsupportedException;

public class ApplicationExceptionDetector extends AbstractEventProcessor{
    
    public static String NO_EXCEPTION_PRINTING = "NO_EXCEPTION_PRINTING";
    
    private static Logger logger = Logger.getLogger(ApplicationExceptionDetector.class);
    private static VMManagerFactory vmmf = VMManagerFactory.getInstance();
    
    private Set<String> applicationClasses = new HashSet<String>();
    private List<IApplicationExceptionListener> listeners = new ArrayList<IApplicationExceptionListener>();
    private DistributedThreadManager dtm;
    private boolean notificationEnabled = false;
    
    public ApplicationExceptionDetector(DistributedThreadManager dtm){ 
        this.dtm = dtm;
    }
    
    public void addApplicationClass(String cls){
        applicationClasses.add(cls);
    }
    
    public boolean removeApplicationClass(String cls){
        return applicationClasses.remove(cls);
    }
    
    public void addApplicationExceptionListener(IApplicationExceptionListener listener){
        listeners.add(listener);
    }
    
    public boolean removeApplicationExceptionListener(IApplicationExceptionListener listener){
        return listeners.remove(listener);
    }
    
    @Override
    public void setDebugContext(IDebugContext dc){
        super.setDebugContext(dc);
        dc.getVotingManager().declareVoterFor(NO_EXCEPTION_PRINTING);
    }
    
    public void specializedProcess(Event e) {
        if (!(e instanceof ExceptionEvent))
            throw new UnsupportedException("Unsupported event type "
                    + e.getClass());
        
        Object marking = e.request().getProperty(ApplicationExceptionDetector.class);
        // Not one of our requests.
        if(marking == null) return;

        /** It's our request. We don't want the other processors printing it out. */
        ProcessingContextManager pcm = ProcessingContextManager.getInstance();
        IProcessingContext ipc = pcm.getProcessingContext();
        ipc.vote(NO_EXCEPTION_PRINTING);
        
        if(!notificationEnabled){
            ipc.vote(IEventManager.RESUME_SET);
            return;
        }
        
        ExceptionEvent exEvent = (ExceptionEvent)e;
        Location loc = exEvent.location();
        /** We don't care about exceptions that don't happen inside the application 
         * classes.
         */
        if(!applicationClasses.contains(loc.declaringType().name())) return;
        
        /** Now we check to see if this exception will be thrown to the ORB. */
        VirtualMachineManager vmm = vmmf.getVMManager((String) exEvent
                .request().getProperty(DebuggerConstants.VMM_KEY));
        IVMThreadManager tm = vmm.getThreadManager();
        ThreadReference tr = exEvent.thread();
        Integer lt_uuid = tm.getThreadUUID(tr);
        if(lt_uuid == null) return;    // Unregistered thread. Forget it.
        
        Integer dt_uuid = dtm.getEnclosingDT(lt_uuid);
        if(dt_uuid == null) return; // This thread is not distributed. Forget it.
        
        DistributedThread dt = dtm.getByUUID(dt_uuid);
        boolean resume = false;
        try{
            dt.lock();
            VirtualStack vs = dt.virtualStack();
            VirtualStackframe head = vs.peek();
            if(!head.getLocalThreadId().equals(lt_uuid)){
                throw new PropertyViolation("Safety property violation - component thread is generating " +
                        "events. Have you disabled all timeouts?");
            }
            /** We have to suspend the head (which is most likely already suspended) in order
             * to inspect its call stack;
             */
            while(!tr.isSuspended()){
                tr.suspend();
                resume = true;
            }
            
            int trFrames;
            try{
                trFrames = tr.frameCount();
            }catch(IncompatibleThreadStateException ex){
                logger.error("Could no acquire frames. Suspension protocol needs improvement.", ex);
                return;
            }
            
            int callBase = head.getCallBase();
            if(callBase == VirtualStackframe.UNDEFINED){
                assert vs.getVirtualFrameCount() == 1; // Has to be the base frame.
                callBase = 1;
            }
            /** If this assertion doesn't hold, something went wrong. */
            assert callBase >= trFrames;
            
            /**
             * Okay, the exception will be thrown to the ORB. We should notify
             * the user about that and print the distributed stack frame.
             */
            if (callBase == trFrames) {
                ObjectReference or = exEvent.exception();
                String theReason = "<nil or unretrievable reason message>";
                try {
                    Method reason = (Method) or.referenceType().methodsByName(
                            "getMessage").iterator().next();
                    StringReference str = (StringReference) or.invokeMethod(tr,
                            reason, new ArrayList(),
                            ClassType.INVOKE_SINGLE_THREADED);
                    theReason = str.value();
                } catch (Exception ex) {
                }

                for (IApplicationExceptionListener listener : listeners) {
                    listener.exceptionOccurred(theReason, or, dt_uuid, lt_uuid);
                }
            }
            
            
        }finally{
            if(resume == true && tr != null) tr.resume();
            dt.unlock();
        }
    }
    
    public void setNotificationEnabled(boolean enabled){
        this.notificationEnabled = enabled;
    }

}
