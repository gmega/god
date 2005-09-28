/*
 * Created on Jan 31, 2005
 * 
 * file: JDIMiscTrait.java
 */
package ddproto1.util.traits;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.request.DeferrableClassPrepareRequest;

/**
 * @author giuliano
 *
 */
public class JDIMiscTrait {
    private static JDIMiscTrait instance = null;

    public synchronized static JDIMiscTrait getInstance(){
        return (instance == null)?(instance = new JDIMiscTrait()):(instance);
    }
    
    private JDIMiscTrait(){ }
    
    public void clearPreviousStepRequests(ThreadReference thread, VirtualMachineManager parent){
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
        
        /** This is part of my hack. Unset the last step request. */
        parent.setLastStepRequest(parent.getThreadManager().getThreadUUID(thread), null);        
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
}
