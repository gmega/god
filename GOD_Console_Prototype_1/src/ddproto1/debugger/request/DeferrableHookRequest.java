/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DeferrableHookRequest.java
 */

package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.jdi.VMDisconnectedException;

import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.commons.UnsupportedException;


/**
 * @author giuliano
 *
 */


public class DeferrableHookRequest implements IDeferrableRequest{

    private String vmid;
    private IJDIEventProcessor iep;
    private int type;
    private Set filters;
    
    private VirtualMachineManager targetVMM;

    private static List precondList;
    private static IPrecondition precondition;
    
    static {
        StdPreconditionImpl sip = new StdPreconditionImpl();
        sip.setClassId(null);
        sip.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        precondition = sip;
        precondList = new ArrayList();
        precondList.add(sip);
    }
    
    public DeferrableHookRequest(String vmid, int type, IJDIEventProcessor iep, Set filters){
         this.vmid = vmid;
         this.type = type;
         this.iep = iep;
         this.filters = filters;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#eagerlyResolve()
     */
    public Object eagerlyResolve() throws Exception {
        /* If this works it possibly means you should be
         * adding your hooks directly to the VM, but we
         * try it anyway to preserve semantics.
         */
        try{
            VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vmid);
            IEventManager iem = vmm.getEventManager();
            
            StdResolutionContextImpl rc = new StdResolutionContextImpl();
            rc.setContext(iem);
            rc.setPrecondition(precondition);
            
            resolveNow(rc);
            
        }catch(VMDisconnectedException e){
            // Nope, sorry.
            return null;
        }

        /* Useless, but conforms to the rule that null is returned
         * only when eagerly resolution cannot be accomplished.
         */
        return new Boolean(true);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#getRequirements()
     */
    public List getRequirements() {
        return precondList;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
     */
    public Object resolveNow(IResolutionContext context) throws Exception {
        targetVMM = (VirtualMachineManager)context.getContext();
        
        IEventManager iem = targetVMM.getEventManager();

        /* Finally. Registers the hook at the event manager */
        iem.addEventListener(type, iep);
        if(filters != null)
            iem.setListenerPolicyFilters(iep, filters);
        
        /* If no exception was thrown, then it worked */
        return new Boolean(true);
    }
    
    public IJDIEventProcessor getProcessor(){
        return iep;
    }
    
    public void undo()
        throws Exception
    {
        IEventManager iem = targetVMM.getEventManager();
        iem.removeEventListener(type, iep);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#addResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void addResolutionListener(IResolutionListener listener) {
        throw new UnsupportedException("Operation not supported for this class.");
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#removeResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void removeResolutionListener(IResolutionListener listener) {
        throw new UnsupportedException("Operation not supported for this class.");
    }
}
