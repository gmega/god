/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DeferrableHookRequest.java
 */

package ddproto1.debugger.request;

import java.util.Set;

import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.IJavaNodeManager;

/**
 * @author giuliano
 *
 */

public class DeferrableHookRequest extends AbstractDeferrableRequest{

    private IJDIEventProcessor iep;
    private int type;
    private Set filters;
    
    private IJavaNodeManager targetVMM;
    
    public DeferrableHookRequest(String vmid, int type, IJDIEventProcessor iep, Set filters){
        super(null);
        this.type = type;
        this.iep = iep;
        this.filters = filters;
        StdPreconditionImpl sip = new StdPreconditionImpl();
        sip.setClassId(null);
        sip.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        this.addRequirement(sip);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
     */
    public synchronized Object resolveInternal(IResolutionContext context) throws Exception {
        
        if(isCancelled()) return REQUEST_CANCELLED;
        
        targetVMM = (IJavaNodeManager)context.getContext();
        
        IEventManager iem = targetVMM.getEventManager();

        /* Finally. Registers the hook at the event manager */
        iem.addEventListener(type, iep);
        if(filters != null)
            iem.setListenerPolicyFilters(iep, filters);
        
        /* If no exception was thrown, then it worked */
        return new Boolean(true);
    }
    
    public IJDIEventProcessor getProcessor(){ return iep; }
    
    private boolean isResolved(){
        return targetVMM != null;
    }
    
    public synchronized void cancelInternal() throws Exception{
        if(isResolved())
            targetVMM.getEventManager().removeEventListener(type, iep);
    }
}
