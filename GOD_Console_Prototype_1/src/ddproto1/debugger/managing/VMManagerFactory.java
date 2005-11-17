/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: VMManagerFactory.java
 */

package ddproto1.debugger.managing;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sun.jdi.VirtualMachine;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.request.AbstractDeferrableRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class VMManagerFactory {
    
    private static final String module = "VMManagerFactory -";
    
    private static VMManagerFactory instance = null;
    
    private Map <String, VirtualMachineManager> vmms = new HashMap<String, VirtualMachineManager>();
    private Map <Byte, String>                  gid2id = new HashMap <Byte, String> ();
    private Map <VirtualMachine, Byte>          vm2gid = new HashMap <VirtualMachine, Byte>();
    
    private static final MessageHandler mh = MessageHandler.getInstance();
    
    private VMManagerFactory() { }
    
    public synchronized static VMManagerFactory getInstance(){
        if(instance == null)
            instance = new VMManagerFactory();
        
        return instance;
    }
        
    public VirtualMachineManager newVMManager(IObjectSpec vmmspec)
    	throws ConfigException, AttributeAccessException, IncarnationException
    {
        String name = vmmspec.getAttribute(IConfigurationConstants.NAME_ATTRIB);
        String gid = vmmspec.getAttribute(IConfigurationConstants.GUID);
        
        if(vmms.containsKey(name) || gid2id.containsKey(gid))
            throw new ConfigException(
                    module
                            + " Cannot instantiate: there's either a VM bound already bound under the Global ID (GID) "
                            + gid + " or named " + name);
            
        IServiceLocator locator;
        
        try{
            locator = (IServiceLocator) Lookup.serviceRegistry()
                .locate("service locator");
            
        }catch(NoSuchSymbolException ex){
            throw new ConfigException("No service locator has been configured.", ex);
        }
        
        VirtualMachineManager vmm = (VirtualMachineManager)locator.incarnate(vmmspec);
        
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
    
    public Iterable<VirtualMachineManager> machineList(){
        return vmms.values();
    }
    
    public Byte getGidByVM(VirtualMachine vm){
        return (Byte)vm2gid.get(vm); 
    }
    
    private class DeferrableRegisterRequest extends AbstractDeferrableRequest{
    
        private Byte gid;
        private VirtualMachine vm;
       
        private DeferrableRegisterRequest(Byte gid){
            super(gid2id.get(gid));
            this.gid = gid;
            StdPreconditionImpl tmp = new StdPreconditionImpl();
            tmp.setClassId(null);
            tmp.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
            this.addRequirement(tmp);
        }

        /* (non-Javadoc)
         * @see ddproto1.debugger.request.IDeferrableRequest#resolveNow(ddproto1.debugger.request.IDeferrableRequest.IResolutionContext)
         */
        public Object resolveInternal(IResolutionContext context) throws Exception {
            VirtualMachineManager vmm = (VirtualMachineManager)context.getContext();
            vm = vmm.virtualMachine();
            vm2gid.put(vm, gid);
            return vm;
        }
        
        public void cancelInternal(){
        	
        }
    }
 
}
