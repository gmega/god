/*
 * Created on Apr 1, 2005
 * 
 * file: DeferrableClassPrepareRequest.java
 */
package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ClassPrepareRequest;

import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.util.collection.AbstractMultiMap;
import ddproto1.util.collection.OrderedMultiMap;
import ddproto1.util.collection.UnorderedMultiMap;


/**
 * Well, though our code never needs to place deferred class prepare requests, this 
 * class does just that. Its main purpose, however, is to comply to the DISALLOW_DUPLICATES
 * policy of class DeferrableRequestQueue. If the user places all of her/his class prepare
 * requests through this class, then it is guaranteed that only one precondition will be 
 * input to the deferrable request queue per class name. So, the though the class allow 
 * deferrable class prepare requests, it does something else - guarantees no duplicate
 * class load preconditions - and that's its main purpose.
 * 
 * If the user doesn't care about duplicates or needs to place multiple prepare requests 
 * per class, he/she can enable duplicates at the request queue and set requests directly
 * through JDI.
 * 
 * 
 * @author giuliano
 *
 */
public class DeferrableClassPrepareRequest implements IDeferrableRequest{
    
    private static List<IDeferrableRequest.IPrecondition> preconds = 
        new ArrayList<IDeferrableRequest.IPrecondition>();
    
    static{
        StdPreconditionImpl prec = new StdPreconditionImpl();
        prec.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        preconds.add(prec);
    }
    
    private static AbstractMultiMap<String, String> reqRepository = 
        new UnorderedMultiMap<String, String>(HashSet.class);
    
    private String vmid;
    private boolean resolved = false;
    
    private List <String> classFilters = new ArrayList<String>();
    private List <String> exclusionFilters = new ArrayList<String>();
    private List <IResolutionListener> listeners = new LinkedList<IResolutionListener>();
        
    public DeferrableClassPrepareRequest(String vmid) { 
        this.vmid = vmid;
    }
    
    public Object resolveNow(IResolutionContext context) throws Exception {
                
        checkResolved();
        
        if(!context.getPrecondition().equals(preconds.get(0)))
            throw new InternalError("Invalid precondition.");
        
        VirtualMachine vm = ((VirtualMachineManager)context.getContext()).virtualMachine();
        
        List <String> toAdd = new ArrayList<String>();
        
        // Cannot change the request anymore.
        synchronized(this){
            
            /* We create the class prepare request here. It might be discarded, but its
             * an acceptable tradeoff to reduce the chances of an exception below the critical
             * line.
             */ 
            ClassPrepareRequest cpr = vm.eventRequestManager().createClassPrepareRequest();

            /**
             * Note that this might be troublesome, especially if an exception is thrown
             * while adding the class filters or enabling the actual class prepare request.
             * That's because we are reporting having done something that we didn't actually do,
             * which is marking as 'requested' class names that weren't actually requested yet.
             * We then make those changes visibile to others to reduce lock contention. 
             * Even so, this code seems to be the most cost-effective way of doing this I 
             * can think about. 
             * 
             * In other words, this should exhibit transactional behavior but it doesn't - if
             * it fails by a runtime exception there'll be no rollback and things will just 
             * blow up. To add the rollback we'd have to hold the 'requested' lock until the 
             * class prepare request got actually resolved, which could degrade performance - 
             * the 'requested' lock crosses all instances of DeferrableClassPrepareRequest.
             */
            synchronized(reqRepository){
                for(String cls : classFilters){
                    if(!reqRepository.contains(vmid, cls)){
                        toAdd.add(cls);
                        reqRepository.add(vmid, cls);
                    }
                }
            }
            
            /* If there's nothing to do but the request had the intention to do something,
             * then we should do nothing.
             */
            if(toAdd.size() == 0 && classFilters.size() != 0) return new Object();

            // -------- Unprogrammed exceptions below this line will cause trouble ----------
            
            for(String cls : toAdd){
                cpr.addClassFilter(cls);
            }
            
            for(String cls : exclusionFilters){
                cpr.addClassExclusionFilter(cls);
            }
        
            /* We have to limit to one. If we allow the user to specify the count filter,
             * there might be conflicts when two requests to addressed at the same class
             * but with different count filters. */
            cpr.addCountFilter(1);
            cpr.enable();
            
            resolved = true;
        
            return cpr;
        }
    }
    
    public synchronized void addClassFilter(String filter){
        checkResolved();
        classFilters.add(filter);
    }
    
    public synchronized void addClassExclusionFilter(String filter){
        checkResolved();
        exclusionFilters.add(filter);
    }

    public synchronized void addResolutionListener(IResolutionListener listener) {
        checkResolved();
        listeners.add(listener);
    }

    public synchronized void removeResolutionListener(IResolutionListener listener) {
        checkResolved();
        listeners.remove(listener);
    }

    public List<IPrecondition> getRequirements() {
        return preconds;
    }
    
    private void checkResolved(){
        if(resolved) throw new IllegalStateException("Request has already been placed.");
    }

}
