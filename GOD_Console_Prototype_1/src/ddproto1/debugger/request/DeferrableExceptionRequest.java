/*
 * Created on Feb 18, 2005
 * 
 * file: DeferrableExceptionRequest.java
 */
package ddproto1.debugger.request;

import java.util.LinkedList;
import java.util.List;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;

import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.util.PolicyManager;


/**
 * Deferrable version of the JDI ExceptionRequest class. It's stripped down
 * because I'm testing wether I can live without the language-specific
 * methods of ExceptionRequest or not.   
 * 
 * @author giuliano
 *
 */
public class DeferrableExceptionRequest implements IDeferrableRequest{

    private boolean caught;
    private boolean uncaught;
    
    private List <String> classFilters = new LinkedList <String> ();
    private List <String> classExclusions = new LinkedList <String> ();
    
    private List <IResolutionListener> listeners = new LinkedList <IResolutionListener> ();
    private List <IPrecondition> preconList = new LinkedList<IPrecondition>();
    
    private static IPrecondition connp;
    
    static {
        StdPreconditionImpl sip = new StdPreconditionImpl();
        sip.setClassId(null);
        sip.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        connp = sip;
    }
        
    public DeferrableExceptionRequest(String vmid, String target, boolean caught, boolean uncaught) {
        this.caught = caught;
        this.uncaught = uncaught;
        
        /* The target class must be loaded. */
        StdPreconditionImpl loadp = new StdPreconditionImpl();
        loadp.setClassId(target);
        loadp.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        preconList.add(connp);
        preconList.add(loadp);
    }
    
    public void addClassExclusionFilter(String classPattern) { 
        classExclusions.add(classPattern);
    }

    public void addClassFilter(String classPattern) {
        classFilters.add(classPattern);
    }
   
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
     */
    public Object resolveNow(IResolutionContext context) throws Exception {
        IDeferrableRequest.IPrecondition precond = context.getPrecondition();
        
        /* If the VM is already running, 
         * we must check if the target class has already been loaded. */
        if(precond.getType().eventType() != IDeferrableRequest.VM_CONNECTION &&
                precond.getType().eventType() != IDeferrableRequest.CLASSLOADING){
            throw new InternalError("Unrecognized precondition.");
        }
        
        /* Well, it doesn't matter which precondition has been reached - the VM should
         * already be connected. */
        VirtualMachineManager vmm = (VirtualMachineManager)context.getContext();
        VirtualMachine vm = vmm.virtualMachine();
        EventRequestManager erm = vm.eventRequestManager();
        
        List <ReferenceType> allException = vm.classesByName(Throwable.class.toString());
        
        /* No classes have been loaded. */
        if(allException.size() == 0){
            /* FIXME We have to change this protocol. It makes no sense to 
             * return a non-null object, it would be better if we could just 
             * return true or false.
             */
            return new Boolean(true);
        }
        
        List <EventRequest> requests = new LinkedList<EventRequest>();
        
        for(ReferenceType target : allException){
            ExceptionRequest trueEr = erm.createExceptionRequest(target, caught, uncaught);
            
            /* Applies the predefined filters. */
            for(String included : classFilters){
                trueEr.addClassFilter(included);
            }
        
            for(String excluded : classExclusions ){
                trueEr.addClassExclusionFilter(excluded);
            }
            
            PolicyManager pm = PolicyManager.getInstance();
            trueEr.setSuspendPolicy(pm.getPolicy("request.exception"));
            trueEr.enable();
            
            requests.add(trueEr);
        }
        
        /* Notifies the resolution of this event request to all registered listeners. */
        for(IResolutionListener listener : listeners){
            listener.notifyResolution(this, requests);
        }
        
        return requests;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#addResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void addResolutionListener(IResolutionListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#removeResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void removeResolutionListener(IResolutionListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#getRequirements()
     */
    public List getRequirements() {
        return preconList;
    }

}
