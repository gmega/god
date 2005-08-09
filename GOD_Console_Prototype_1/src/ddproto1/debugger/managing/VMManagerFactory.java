/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: VMManagerFactory.java
 */

package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;

import ddproto1.configurator.IConfigurable;
import ddproto1.configurator.newimpl.IConfigurationConstants;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.debugger.request.IDeferrableRequest.IPrecondition;
import ddproto1.exception.AttributeAccessException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class VMManagerFactory {
    
    private static final String module = "VMManagerFactory -";
    
    private static VMManagerFactory instance = null;
    
    private IJDIEventProcessor proc = null;
    
    private Map vmms = new HashMap();
    private Map gid2id = new HashMap();
    private Map vm2gid = new HashMap();
    
    private static final MessageHandler mh = MessageHandler.getInstance();
    
    private VMManagerFactory() { }
    
    public synchronized static VMManagerFactory getInstance(){
        if(instance == null)
            instance = new VMManagerFactory();
        
        return instance;
    }
        
    public VirtualMachineManager newVMManager(IObjectSpec info)
    	throws ConfigException, AttributeAccessException
    {
        String name = info.getAttribute(IConfigurationConstants.NAME_ATTRIB);
        String gid = info.getAttribute(IConfigurationConstants.GUID);
        if(vmms.containsKey(name) || gid2id.containsKey(gid))
            throw new ConfigException(
                    module
                            + " Cannot instantiate: there's either a VM bound already bound under the Global ID (GID) "
                            + gid + " or named " + name);
               
        VirtualMachineManager vmm = new VirtualMachineManager(info);
        vmms.put(name, vmm);
        gid2id.put(new Byte(gid), name);

        DeferrableRegisterRequest req = new DeferrableRegisterRequest(new Byte(gid));
        
        try{
            vmm.getDeferrableRequestQueue().addEagerlyResolve(req);
        }catch(Exception e){
            mh.printStackTrace(e);
        }
               
        return vmm;
    }
    
    public VirtualMachineManager getVMManager(String id)
    {
        if(!vmms.containsKey(id))
            throw new NoSuchElementException("Invalid id - " + id);
            
        return (VirtualMachineManager)vmms.get(id);
    }
    
    public VirtualMachineManager getVMManager(Byte gid){
        if(!gid2id.containsKey(gid))
            throw new NoSuchElementException("Invalid gid - " + gid);
        
        return((VirtualMachineManager)vmms.get(gid2id.get(gid)));
    }
    
    public Iterator machineList(){
        return vmms.values().iterator();
    }
    
    public Byte getGidByVM(VirtualMachine vm){
        return (Byte)vm2gid.get(vm); 
    }
    
    /* These static fields should belong to class DeferrableRegisterRequest but
     * since java does not allow it we've moved them here.
     */
    private static IPrecondition precondition;
    private static List preconList;
    
    static{
        StdPreconditionImpl tmp = new StdPreconditionImpl();
        tmp.setClassId(null);
        tmp.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        precondition = tmp;
        preconList = new ArrayList();
        preconList.add(tmp);
    }
    
    private class DeferrableRegisterRequest implements IDeferrableRequest{
    
        private Byte gid;
        private VirtualMachine vm;
                
        
        private DeferrableRegisterRequest(Byte gid){
            this.gid = gid;
        }
        
        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#eagerlyResolve()
         */
        public Object eagerlyResolve() throws Exception {
            VirtualMachineManager vmm = getVMManager(gid);
            // Should never happen.
            assert(vmm != null);
            
            try{
                vm = vmm.virtualMachine();
                
                /* Might sound stupid that we are creating an entire
                 * ResolutionContext just for passing it as a parameter
                 * to a method that returns something we already have, 
                 * but all for the sake of preserving semantics.
                 */
                IResolutionContext ir = new IResolutionContext(){

                    public IPrecondition getPrecondition() {
                        return precondition;
                    }

                    public Object getContext() {
                        return vm;
                    }
                };
                
                return this.resolveNow(ir);
                
            }catch(VMDisconnectedException e){
                return null;
            }
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
         */
        public Object resolveNow(IResolutionContext context) throws Exception {
            VirtualMachineManager vmm = (VirtualMachineManager)context.getContext();
            vm = vmm.virtualMachine();
            vm2gid.put(vm, gid);
            return vm;
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#getRequirements()
         */
        public List getRequirements() {
            return preconList;
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
 
}
