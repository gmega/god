/*
 * Created on Feb 18, 2005
 * 
 * file: DeferrableExceptionRequest.java
 */
package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.JDIMiscTrait;


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
    private int countFilter = DebuggerConstants.UNKNOWN;
    
    private List <String> classFilters = new LinkedList <String> ();
    private List <String> classExclusions = new LinkedList <String> ();
    
    private List <String> pendingLoads = new LinkedList<String> ();
    private List <ReferenceType> loadedClasses = new LinkedList<ReferenceType> ();
    
    private List <IResolutionListener> listeners = new LinkedList <IResolutionListener> ();
    private List <IPrecondition> preconList = new LinkedList<IPrecondition>();
    
    private Map<Object, Object> properties = new HashMap<Object, Object>();
    
    private String vmid;
    private String targetException;
    
    public DeferrableExceptionRequest(String vmid, String targetException,
            List<String> realClassFilters, boolean caught, boolean uncaught) {
        this.caught = caught;
        this.uncaught = uncaught;
        this.vmid = vmid;
        this.targetException = targetException;
        
        /* The only precondition is that the target exception class must be loaded. */
        StdPreconditionImpl loadp = new StdPreconditionImpl();
        loadp.setClassId(targetException);
        loadp.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        preconList.add(loadp);
        pendingLoads.add(targetException);
        
        for(String realClass : realClassFilters){
            StdPreconditionImpl rClassLoad = new StdPreconditionImpl();
            rClassLoad.setClassId(realClass);
            rClassLoad.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
            preconList.add(rClassLoad);
            pendingLoads.add(realClass);
        }
        
        StdPreconditionImpl startVM = new StdPreconditionImpl();
        startVM.setClassId(null);
        startVM.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        preconList.add(startVM);
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
    public Object resolveNow(IResolutionContext context) throws Exception {
        IDeferrableRequest.IPrecondition precond = context.getPrecondition();
        
        if(precond.getType().eventType() == IDeferrableRequest.VM_CONNECTION){
            /** We got connection. Checks which classes have already been loaded. */
            JDIMiscTrait jmt = JDIMiscTrait.getInstance(); 
            VirtualMachineManager vmm = (VirtualMachineManager)context.getContext();
            
            /** Places class prepare requests for everyone */
            List<DeferrableClassPrepareRequest> requests = new ArrayList<DeferrableClassPrepareRequest>();
            for(String clsName : pendingLoads){
                DeferrableClassPrepareRequest dcpr = new DeferrableClassPrepareRequest(vmm.getName());
                dcpr.addClassFilter(clsName);
                requests.add(dcpr);
            }
            /** Now we make the actual requests. This might even resolve ourselves. */
            for(DeferrableClassPrepareRequest dcpr : requests) vmm.getDeferrableRequestQueue().addEagerlyResolve(dcpr);
                        
            return new Boolean(true);
            
        }else if(precond.getType().eventType() == IDeferrableRequest.CLASSLOADING){
            String loadedClass = precond.getClassId();
            if(!pendingLoads.contains(loadedClass)) 
                throw new InternalError("Received load notification for an unwanted class.");
            
            ReferenceType currentClass = (ReferenceType)context.getContext();
            loadedClasses.add(currentClass);
            if(!(pendingLoads.size() == loadedClasses.size())) return new Boolean(true); 

        }else{
            throw new InternalError("Unrecognized precondition.");
        }
        
        
        /** All classes has been loaded, we place our exception request. */
        ReferenceType exceptionClass = this.getExceptionClass();
        ExceptionRequest er = exceptionClass.virtualMachine()
                .eventRequestManager().createExceptionRequest(exceptionClass, caught, uncaught);
        
        /** Apply filters. */
        for(ReferenceType realClass : loadedClasses) er.addClassFilter(realClass);
        for(String exclusion : classExclusions) er.addClassExclusionFilter(exclusion);
        for(String inclusion : classFilters) er.addClassFilter(inclusion);
        if(countFilter != DebuggerConstants.UNKNOWN) er.addCountFilter(countFilter);

        er.putProperty(DebuggerConstants.VMM_KEY, vmid);
        
        for(Object key : properties.keySet())
            er.putProperty(key, properties.get(key));
        er.setSuspendPolicy(PolicyManager.getInstance().getPolicy("request.exception"));
        er.enable();
        return er;
    }
    
    private ReferenceType getExceptionClass(){
        for(Iterator<ReferenceType> it = loadedClasses.iterator(); it.hasNext();){
            ReferenceType rType = it.next();
            if(rType.name().equals(targetException)){
                it.remove();
                return rType;
            }
        }
        
        return null;
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
    
    public void setProperty(Object key, Object value){
        properties.put(key, value);
    }

}
