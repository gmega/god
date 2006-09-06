/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: VMManagerFactory.java
 */

package ddproto1.debugger.managing;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.debug.core.ILaunch;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.VMDeathEvent;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.identification.DefaultGUIDManager;
import ddproto1.debugger.managing.identification.IGUIDManager;
import ddproto1.debugger.request.AbstractDeferrableRequest;
import ddproto1.debugger.request.DeferrableHookRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.JDIEventProcessorTrait;
import ddproto1.util.traits.JDIEventProcessorTrait.JDIEventProcessorTraitImplementor;

/**
 * @author giuliano
 *
 */
public class VMManagerFactory implements INodeManagerFactory, IJavaNodeManagerRegistry {
    
    private static VMManagerFactory instance;
    
    /** I have this reference as a hack to the fact
     * that I can't mock VMManagerFactory.
     * When I move node registry management to the plug-in class
     * and make things more generic, I won't need this anymore.
     */
    private static IJavaNodeManagerRegistry nmrInstance;
    
    private final Map <String, VirtualMachineManager> vmms = 
        Collections.synchronizedMap(new HashMap<String, VirtualMachineManager>());
    private final Map <Byte, String> gid2id = 
        Collections.synchronizedMap(new HashMap <Byte, String> ());
    private final Map <VirtualMachine, Byte> vm2gid = 
        Collections.synchronizedMap(new HashMap <VirtualMachine, Byte> ());

    private static final MessageHandler mh = MessageHandler.getInstance();
    
    private VMManagerFactory() { }
    
    public synchronized static VMManagerFactory getInstance(){
        if(instance == null) instance = new VMManagerFactory();
        return instance;
    }
    
    public synchronized static IJavaNodeManagerRegistry getRegistryManagerInstance(){
        if(nmrInstance == null) nmrInstance = getInstance();
        return nmrInstance;
    }
    
    /**
     * This method is for testing purposes only. It exists 
     * because the current design is based on singletons, which
     * are a poor choice for what I must achieve.
     * 
     * @param testDelegate
     */
    public synchronized static void setInstance(IJavaNodeManagerRegistry testDelegate){
        nmrInstance = testDelegate;
    }
        
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMManagerFactory#newVMManager(ddproto1.configurator.IObjectSpec)
     */
    public VirtualMachineManager createNodeManager(IObjectSpec vmmspec)
    		throws ConfigException, AttributeAccessException, IncarnationException {
		/**
		 * First tries to acquire a reference to the service locator service.
		 */
		IServiceLocator locator;
		try {
			locator = (IServiceLocator) Lookup.serviceRegistry().locate(
					"service locator");

		} catch (NoSuchSymbolException ex) {
			throw new ConfigException(
					"No service locator has been configured.", ex);
		}

        synchronized (vmms) {
            /** Checks if this name has already been used. */
            String name = vmmspec
                    .getAttribute(IConfigurationConstants.NAME_ATTRIB);
            if (vmms.containsKey(name))
                throw new ConfigException(
                        "Node Manager names must be unique. (" + name + ")");

            /** Now checks if this debuggee has been leased an ID. */
            Integer gid = GODBasePlugin.getDefault().debuggeeGUIDManager()
                    .currentlyLeasedGUID(vmmspec);
            if (gid == null)
                throw new ConfigException("Debuggee must have a GUID!");
            else if (gid >= Byte.MAX_VALUE - Byte.MIN_VALUE)
                throw new InternalError(
                        "Debuggee GUID manager has been incorrectly configured.");
            Byte _gid = new Byte((byte) ((int) gid));

            /**
             * Finally, incarnates the node manager and adds it to the internal
             * tables.
             */
            try {
                VirtualMachineManager vmm;
                vmm = (VirtualMachineManager) locator.incarnate(vmmspec);
                vmm.setAttribute(IConfigurationConstants.GUID_ATTRIBUTE, _gid
                        .toString());

                /**
                 * This request will bind the VM mirror object to it's ID when
                 * it finally connects.
                 */
                DeferrableRegisterRequest mapVMRef2GUIDRequest = new DeferrableRegisterRequest(
                        _gid);
                vmm.getDeferrableRequestQueue().addEagerlyResolve(
                        mapVMRef2GUIDRequest);

                synchronized (gid2id) {
                    vmm.setParentVMManagerFactory(this);
                    if (gid2id.containsKey(_gid))
                        throw new ConfigException(
                                "Duplicate node numeric ID detected. Cannot create node manager.");
                    vmms.put(name, vmm);
                    gid2id.put(_gid, name);
                }

                return vmm;
            } catch (Exception e) {
                mh.printStackTrace(e);
                throw new IncarnationException(
                        "Failed to resolve register request.");
            }
        }
    }
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see ddproto1.debugger.managing.IVMManagerFactory#getVMManager(java.lang.String)
	 */
    public IJavaNodeManager getNodeManager(String id)
    {
        return getJavaNodeManager(id);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IVMManagerFactory#getVMManager(java.lang.Byte)
     */
    public ILocalNodeManager getNodeManager(Byte gid){
        return getJavaNodeManager(gid);
    }
    
    public IJavaNodeManager getJavaNodeManager(String id)
    {
        synchronized(vmms){
            if(!vmms.containsKey(id))
                throw new NoSuchElementException("Invalid id - " + id);
            
            return (VirtualMachineManager)vmms.get(id);
        }
    }

    public IJavaNodeManager getJavaNodeManager(Byte gid){
        synchronized(gid2id){
            String id = gid2id.get(gid);
            if(id == null)
                throw new NoSuchElementException("Invalid gid - " + gid);
            return getJavaNodeManager(id);
        }
    }

    public IJavaNodeManager getJavaNodeManager(VirtualMachine vm) {
        synchronized(vm2gid){
            Byte gid = vm2gid.get(vm);
            if(gid == null)
                throw new NoSuchElementException();
            return getJavaNodeManager(gid);
        }
    }
    
    public void notifyNodeDeath(IJavaNodeManager jnm){
        synchronized(vm2gid){ // getJavaNodeManager(VirtualMachine vm)
           synchronized(gid2id){ // getJavaNodeManager(Byte gid)
               synchronized(vmms){ // getJavaNodeManager(String s)
                   VirtualMachine vm = jnm.virtualMachine();
                   String name = gid2id.remove(vm2gid.remove(vm));
                   vmms.remove(name);
                   gid2id.remove(name);
               }
           }
        }
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
        
        public void cancelInternal(){ }
    }
}
