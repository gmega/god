/*
 * Created on Feb 18, 2005
 * 
 * file: DeferrableExceptionRequest.java
 */
package ddproto1.debugger.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.commons.ConversionTrait;


/**
 * Deferrable version of the JDI ExceptionRequest class. It's stripped down
 * because I'm testing wether I can live without the language-specific
 * methods of ExceptionRequest or not.   
 * 
 * 17/05/2005 - 
 * 
 * Complexity was getting unmanageable because this class wanted to place 
 * requests per reference type. As a result, it had to manage class prepare
 * requests for an unlimited number of classes, which of course is not very
 * nice. I removed that support. If I need that in the future, I may try to
 * add it again in a smarter way.
 * 
 * This means that if you want to place an exception request that covers a 
 * type and all of its N subtypes, you'll have to create N requests. 
 * 
 * @author giuliano
 *
 */
public class DeferrableExceptionRequest extends AbstractDeferrableRequest{

    private static ConversionTrait ct = ConversionTrait.getInstance();
    
    private boolean caught;
    private boolean uncaught;
    private int countFilter = DebuggerConstants.UNKNOWN;
    
    /** Restrict exception to these classes. */
    private List <String> classFilters = new LinkedList <String> ();
    private List <String> classExclusions = new LinkedList <String> ();
    
    private Map<Object, Object> properties = new HashMap<Object, Object>();
    private Set<EventRequest>   resolvedRequests = new HashSet<EventRequest>();
    
    private String targetException;
    
    public DeferrableExceptionRequest(String vmid, String targetException, 
            boolean caught, boolean uncaught) {
        super(vmid);
        this.caught = caught;
        this.uncaught = uncaught;
        this.targetException = targetException;
        
        if (!ct.isClassName(targetException))
            throw new IllegalArgumentException(targetException
                    + " is not a valid class name.");
              
        /* Precondition 1 - JVM must be running. This is a "match once" precondition */
        StdPreconditionImpl startVM = new StdPreconditionImpl();
        startVM.setClassId(null);
        startVM.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        this.addRequirement(startVM);
       
        /* Precondition 2 - Target exception class must be loaded. This is a "match multiple" precondition. */
        StdPreconditionImpl loadp = new StdPreconditionImpl();
        loadp.setClassId(targetException);
        loadp.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE)); // FIXME - it's match multiple.
        this.addRequirement(loadp);
    }
    
    public void addClassExclusionFilter(String classPattern) { 
        classExclusions.add(classPattern);
    }

    public void addClassFilter(String classPattern) {
        classFilters.add(classPattern);
    }
    
    public void setCountFilter(int countFilter){
        this.countFilter = countFilter;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
     */
    public synchronized Object resolveInternal(IResolutionContext context) throws Exception {
        IDeferrableRequest.IPrecondition precond = context.getPrecondition();
        
        ReferenceType exceptionClass;
        
        /** Precondition 1 met - We got connection. Checks which classes have already been loaded. */ 
        if(precond.getType().eventType() == IDeferrableRequest.VM_CONNECTION){
            VirtualMachineManager vmm = (VirtualMachineManager)context.getContext();
            
            DeferrableClassPrepareRequest dcpr = new DeferrableClassPrepareRequest(vmm.getName());
            dcpr.addClassFilter(targetException);
            
            /** This might unwind into our own resolution */
            vmm.getDeferrableRequestQueue().addEagerlyResolve(dcpr);

            /** Done with this precondition. */
            return OK;
           
        }else if(precond.getType().eventType() == IDeferrableRequest.CLASSLOADING){
            String loadedClass = precond.getClassId();
            
            if(!loadedClass.equals(targetException)) 
                throw new InternalError("Received load notification for an unwanted class.");
        
            /** Precondition 2 met - Target exception has been loaded. */
            exceptionClass = (ReferenceType)context.getContext();

        }else{
            throw new InternalError("Unrecognized precondition.");
        }
        
        
        /** Target exception has been loaded. */
        ExceptionRequest er = exceptionClass.virtualMachine()
                .eventRequestManager().createExceptionRequest(exceptionClass, caught, uncaught);
        
        /** Apply filters. */
        for(String exclusion : classExclusions) er.addClassExclusionFilter(exclusion);
        for(String inclusion : classFilters) er.addClassFilter(inclusion);
        if(countFilter != DebuggerConstants.UNKNOWN) er.addCountFilter(countFilter);

        er.putProperty(DebuggerConstants.VMM_KEY, this.getVMM().getName());
        
        for(Object key : properties.keySet())
            er.putProperty(key, properties.get(key));
        er.setSuspendPolicy(PolicyManager.getInstance().getPolicy("request.exception"));
        this.broadcastToListeners(er);
        er.enable();
        
        resolvedRequests.add(er);
        
        return OK;
    }
    
    public void setProperty(Object key, Object value){
        properties.put(key, value);
    }

    public synchronized void cancelInternal() throws Exception {
        for(EventRequest er : resolvedRequests)
            this.getVMM().virtualMachine().eventRequestManager().deleteEventRequest(er);
    }

}
