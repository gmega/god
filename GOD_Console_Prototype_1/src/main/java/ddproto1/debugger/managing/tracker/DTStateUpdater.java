/*
 * Created on Feb 17, 2005
 * 
 * file: DTStateUpdater.java
 */
package ddproto1.debugger.managing.tracker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.request.ExceptionRequest;

import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.exception.PropertyViolation;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.util.JDIMiscUtil;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * This event processor will convey the information that comes with each
 * thread-related JDI event through the JDI event dispatcher to the associated
 * Distributed Thread as necessary. It also deals with damaging events, capable
 * of breaking the distributed thread's state.
 * 
 * Contrary to many of the others JDIEventProcessors, this event processor will
 * service multiple machines. Additional care must be taken, therefore, as this
 * processor's "process" method might be accessed by potentially many threads.
 * 
 * REMARK The adequate recovery measures are remarkably difficult to define, as
 * well as the resulting state after a damaging event. 
 * This processor is possibly going to grow and change a lot in time. That's
 * why it's so important to keep it's change record. 
 * 
 * What does it do:
 * 
 * 17/02/2005 - Updates the distributed thread state whenever it's head hits
 *              a breakpoint.
 *            - Updates the distributed thread stack whenever it's head dies.
 *            - By monitoring breakpoint and thread death events, it enforces 
 *              an important safety property:
 *              1) If the local thread that has died or has hit a breakpoint is
 *                 part of a distributed thread and is found not to be it's head 
 *                 thread, an error must be reported and the distributed thread 
 *                 marked as damaged.
 * 
 *            - TODO Must update state when VMDeath or VMDisconnected events occur 
 *                   and mark the corresponding threads as damaged.
 * 
 *               
 * 
 * @author giuliano
 * 
 * @deprecated I'm not going to invest into this class anymore. DistributedThread now
 * warrants its own safety, local threads are in charge of notifying DistributedThread
 * of state changes. 
 *
 */
public class DTStateUpdater implements IJDIEventProcessor, IResolutionListener {

    private static Logger logger = Logger.getLogger(DTStateUpdater.class);
    
    private static final IVMManagerFactory vmmf = VMManagerFactory.getInstance();
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    private DistributedThreadManager dtm;
    private boolean enabled = true;
    private IJDIEventProcessor next;
    
    private Set <ExceptionRequest> exReqs = new HashSet <ExceptionRequest> ();
    
    public DTStateUpdater(DistributedThreadManager dtm) {
        this.dtm = dtm;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#process(com.sun.jdi.event.Event)
     */
    public synchronized void process(Event e) {
        if(enabled){
            if((e instanceof BreakpointEvent) || (e instanceof StepEvent))
                updateState((LocatableEvent)e);
            else if(e instanceof ThreadDeathEvent)
                updateState((ThreadDeathEvent)e);
        }
        if(next != null) next.process(e);
    }
    
    /**
     * Updates the state of the distributed thread encompassing e.thread()
     * whenever a breakpoint gets hit.
     * 
     */
    private void updateState(LocatableEvent e){
        ThreadReference tr = e.thread();
        DistributedThread dt = acquireDT(tr);
        if(dt == null) return;

        /* If we got this far it means the DistributedThreadManager 
         * has reported that the local thread which has hit the breakpoint
         * is currently part of a distributed thread (dt).
         */

        try{
            /* Lock the dt reference. */
            dt.lock();
            
            /* Starts by testing if the activity took place in the
             * dt's head thread.
             */
            DistributedThread.VirtualStack vs = dt.virtualStack();
            VirtualStackframe vsf = vs.peek();
            Integer lt_uuid = getID(e.thread());

            /* All ok. */
            if(lt_uuid.equals(vsf.getLocalThreadId())){
                
                /* The rule is as follows:
                 * If we detect that the thread escaped the debugger control area,
                 * we give up the distributed illusion.
                 */
                try {
                    int callStackLength = tr.frameCount();
                    /**
                     * If the event has been generated below the call to the
                     * first dispatched POA call...
                     */
                    byte modifier;
                    /** ... we remove the illusion. */
                    if (callStackLength < vsf.getCallBase()) {
                        modifier = 0;
                    } 
                    /** Otherwise, we keep it. */
                    else {
                        modifier = DistributedThread.ILLUSION;
                    }
                    
                    if (e instanceof StepEvent) {
                        dt.setStepping((byte)(modifier | DistributedThread.STEPPING));
                    } else {
                    	List<VirtualStackframe> frames = vs.virtualFrames(0, vs.getVirtualFrameCount());
                        /** We must clear all pending step requests for this distributed thread,
                         * because there might be a pending step over somewhere along the stack. */
                        for(VirtualStackframe frame : frames){
                        	if(frame.isCleared()) continue;
                        	IJavaNodeManager vmm = (IJavaNodeManager)VMManagerFactory.getInstance().
                                    getNodeManager(frame.getLocalThreadNodeGID()).getAdapter(IJavaNodeManager.class);
                        	ThreadReference target = vmm.getThreadManager().findThreadByUUID(frame.getLocalThreadId());
                        	JDIMiscUtil.getInstance().clearPreviousStepRequests(target, vmm);
                        	frame.clear();
                        }

                        /** It's not really necessary to OR the modifier here as the current
                         * version re-enables remote mode automatically when that's possible.
                         * We should allow the user to configure that behavior, however, and this
                         * modifier is a reminder of that.
                         */
                        dt.setStepping((byte)(DistributedThread.SUSPENDED | modifier));
                    }
                    
            
                
                } catch (IncompatibleThreadStateException ex) {
                    logger.error("Failed to acquire the call stack. "
                            + "\n Local thread id: " + ct.uuid2Dotted(lt_uuid)
                            + "\n Distributed thread id: " + dt.getId()
                            + "\n State may not be updated correctly.");
                } catch (InvalidAttributeValueException ex) {
                    throw new InternalError(
                            "DTStateUpdater attempted to set a state that does not exist.");
                }
            }
            /*
             * Some sort of inconsistency occurred - either the
             * DistributedThreadManager (DTM) unnacurately reported that the
             * current distributed thread encom- passes the local event thread
             * or there has been a safety property violation.
             */
            else {
                String violation = "unknown";

                /*
                 * If lt_uuid == null or lt_uuid is not found to be in dt's
                 * stack, it probably means the DTM has reported unnacurate information.
                 * This sort of a serious error and probably indicates issues
                 * with the internal logic. It doesn't necessarily mean,
                 * however, that the dt state is damaged, though it doesn't mean
                 * it's not damaged as well, it's just that we can't assess it.
                 */
                violation = "The distributed thread manager wrongly reported "
                        + "an unregistered thread as being part of distributed thread "
                        + ct.uuid2Dotted(dt.getId());

                if (lt_uuid != null) {
                    /*
                     * Tries to see if at least the thread is actually part of
                     * the distributed thread reported by the DTM.
                     */

                    int damaged = findDamagedFrame(vs, lt_uuid);

                    if (damaged != -1) {
                        violation = "Safety Property Violation - Component thread "
                                + ct.uuid2Dotted(lt_uuid.intValue())
                                + " is executing even tough it should be blocked.";

                        dt.setDamaged(true, damaged);
                    }
                }

                throw new PropertyViolation(violation);
            }

        } finally {
            dt.unlock();
        }
    }
    
    private void updateState(ThreadDeathEvent e){
        DistributedThread dt = acquireDT(e.thread());
        if(dt == null) return;
        
        try{
            dt.lock();
            
            /* Starts by testing if the activity took place in the
             * dt's head thread.
             */
            DistributedThread.VirtualStack vs = dt.virtualStack();
            VirtualStackframe vsf = vs.peek();
            Integer lt_uuid = getID(e.thread());

            /* Head is dead. We must pop it. Chained exceptions could
             * really mess things up but i have to test for that yet. 
             * This is a recoverable, expected situation and technically 
             * the dt's state is not damaged. Yet.*/
            if(lt_uuid.equals(vsf.getLocalThreadId())){
                // If the root is dead, the DT is dead as well.
                if(vs.getVirtualFrameCount() == 1){
                    dtm.notifyDeath(lt_uuid); // no problem. Root means lt_uuid equals dt_uuid.
                } else
                    vs.popFrame();
                return;
            }
            /* It's worse, much worse. A thread in the middle has died. This
             * could potentially mess up everything about this thread. OR, the
             * DTM reported innacurate information. Either way, there's been
             * a violation.
             */
            else{
                String violation = "The distributed thread manager wrongly reported "
                    + "an unregistered thread as being part of distributed thread "
                    + ct.uuid2Dotted(dt.getId());

                if(lt_uuid != null){
                    int damaged = findDamagedFrame(vs, lt_uuid);
                
                    if(damaged != -1){
                        violation = "Safety Property Violation - Component thread "
                            + ct.uuid2Dotted(lt_uuid.intValue())
                            + " died and broke a distributed thread.";
                        
                        dt.setDamaged(true, damaged);
                    }
                }
                
                throw new PropertyViolation(violation);
            }
        
        }finally{
            dt.unlock();
        }
    }
    
    private int findDamagedFrame(DistributedThread.VirtualStack vs, Integer lt_uuid) {

        int idx = -1;
        /*
         * Tries to see if at least the thread is actually part of the
         * distributed thread reported by the DTM.
         * 
         * The loop scans the dt's virtual stack and looks for it.
         */
        for (int i = 0; i < vs.getVirtualFrameCount(); i++) {
            VirtualStackframe vsf = vs.getVirtualFrame(i);
            if (vsf.getLocalThreadId().equals(lt_uuid)) {
                idx = i;
                break;
            }
        }
        
        return idx;
    }
    
    private DistributedThread acquireDT(ThreadReference tr){

        Integer lt_uuid = this.getID(tr);
        
        // Not a registered thread. We're not interested.
        if(lt_uuid == null)
            return null;
        
        Integer dt_uuid = dtm.getEnclosingDT(lt_uuid);
        if(dt_uuid == null) return null;
        
        DistributedThread dt = dtm.getByUUID(dt_uuid);
        
        return dt;
    }
    
    private Integer getID(ThreadReference tr){
        IJavaNodeManager vmm = (IJavaNodeManager)vmmf.
            getNodeManager(vmmf.getGidByVM(tr.virtualMachine())).getAdapter(IJavaNodeManager.class);
        Integer lt_uuid = vmm.getThreadManager().getThreadUUID(tr);
        return lt_uuid;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#setNext(ddproto1.debugger.eventhandler.processors.IJDIEventProcessor)
     */
    public void setNext(IJDIEventProcessor iep) {
        this.next = iep;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#getNext()
     */
    public IJDIEventProcessor getNext() {
        return next;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#enable(boolean)
     */
    public void enable(boolean status) {
        enabled = status;
    }

    public synchronized void notifyResolution(IDeferrableRequest source, Object byproduct) {
        exReqs.add((ExceptionRequest)byproduct);
    }

}
