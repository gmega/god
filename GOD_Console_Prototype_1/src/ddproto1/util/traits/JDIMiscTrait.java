/*
 * Created on Jan 31, 2005
 * 
 * file: JDIMiscTrait.java
 */
package ddproto1.util.traits;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.request.DeferrableClassPrepareRequest;
import ddproto1.util.traits.commons.ConversionTrait;

/**
 * @author giuliano
 *
 */
public class JDIMiscTrait {
    private static JDIMiscTrait instance = null;
    private static Logger remLogger = Logger.getLogger(JDIMiscTrait.class.getName() + ".removalLogger");

    public synchronized static JDIMiscTrait getInstance(){
        return (instance == null)?(instance = new JDIMiscTrait()):(instance);
    }
    
    private JDIMiscTrait(){ }
    
    public void clearPreviousStepRequests(ThreadReference thread, VirtualMachineManager parent){
    	if(remLogger.isDebugEnabled()){
    		String theId = "non-registered";
    		Integer id = parent.getThreadManager().getThreadUUID(thread);
    		if(id != null) theId = ConversionTrait.getInstance().uuid2Dotted(id);
    		
    		remLogger.debug("Removing step requests. \n" +
    				"Thread id: " + theId);
    	}
        EventRequestManager mgr = thread.virtualMachine().eventRequestManager();
        List requests = mgr.stepRequests();
        Iterator iter = requests.iterator();
        while (iter.hasNext()) {
            StepRequest request = (StepRequest) iter.next();
            if (request.thread().equals(thread)) {
                mgr.deleteEventRequest(request);
                break;
            }
        }
    }
    
    public List<ReferenceType> getLoadedClassesFrom(VirtualMachineManager vmm, List<String> typeNames){
        List<ReferenceType> loadedTypes = new ArrayList<ReferenceType>();
        VirtualMachine vm = vmm.virtualMachine();
        for(String typeName : typeNames){
            loadedTypes.addAll(vm.classesByName(typeName));
        }
        
        return loadedTypes;
    }
    
    public List<ReferenceType> getLoadedClassesFrom(VirtualMachineManager vmm, String typeName){
        VirtualMachine vm = vmm.virtualMachine();
        return vm.classesByName(typeName);
    }
   
    public void createClassPrepareRequests(VirtualMachineManager vmm, List<String> classes)
        throws Exception
    {
        for(String clsName : classes){
            DeferrableClassPrepareRequest cpr = new DeferrableClassPrepareRequest(vmm.getName());
            cpr.addClassFilter(clsName);
            vmm.getDeferrableRequestQueue().addEagerlyResolve(cpr);
        }
    }
    
    public Byte getPendigStepRequestMode(int lt_uuid, VirtualMachineManager vmm, boolean overIsRunning){
    	ThreadReference tr = vmm.getThreadManager().findThreadByUUID(lt_uuid);
    	List<StepRequest> srs = vmm.virtualMachine().eventRequestManager().stepRequests();
    	for(StepRequest sr : srs){
    		if(sr.thread().equals(tr)) 
    			return (sr.depth() == StepRequest.STEP_INTO)?DebuggerConstants.STEPPING:((overIsRunning)?DebuggerConstants.RUNNING:DebuggerConstants.STEPPING);
    	}
    	
    	return null;
    }
}
