/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: VirtualMachineManager.java
 */

package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.Mirror;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.EventDispatcher;
import ddproto1.debugger.eventhandler.DelegatingHandler;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IVotingManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.eventhandler.processors.ClientSideThreadStopper;
import ddproto1.debugger.eventhandler.processors.DeferredEventExecutor;
import ddproto1.debugger.eventhandler.processors.ExceptionHandler;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.eventhandler.processors.ResumingChainTerminator;
import ddproto1.debugger.eventhandler.processors.SourcePrinter;
import ddproto1.debugger.eventhandler.processors.StepRequestClearer;
import ddproto1.debugger.eventhandler.processors.ThreadInfoGatherer;
import ddproto1.debugger.eventhandler.processors.ThreadUpdater;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdResolutionContextImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.exception.IllegalStateException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.sourcemapper.ISourceMapper;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;


/**
 * This class groups the infrastructure required to manage
 * events and communication with an individual JVM. It also
 * acts as a fa�ade for placing requests.
 * 
 * Responsible for assembling Dispatchers, Handlers and IJDIEventProcessor
 * chains.
 * 
 * @author giuliano
 *
 */
public class VirtualMachineManager implements IJDIEventProcessor, Mirror, IConfigurable{
    
    private static final String module = "VirtualMachineManager -";

    private DeferrableRequestQueue queue;
    private EventDispatcher disp = null;
    private DelegatingHandler handler;
    private ISourceMapper smapper;
    private VirtualMachine jvm;
    private ThreadManager tm;
    private AbstractEventProcessor aed;
    
    private IJDIEventProcessor next;
    
    /** HACK MAP. */
    private Map<Integer, Byte> stepRequests = new HashMap<Integer, Byte>();
    
    private String name;
    private String gid;
    
    /** Creates a new VirtualMachineManager.
     * 
     * @param info Virtual machine specification.
     * 
     */
    protected VirtualMachineManager() 
    	throws AttributeAccessException
    {
        this.handler = new DelegatingHandler();
        this.disp = new EventDispatcher(handler);
        
        /*TODO When code hits beta, enable the disallow duplicates. */
        //queue = new DeferrableRequestQueue(name, DeferrableRequestQueue.DISALLOW_DUPLICATES);
    }

    public DeferrableRequestQueue getDeferrableRequestQueue(){
        return queue;
    }
    
    /** Returns the associated JVM mirror.
     * 
     * @return
     * @throws VMDisconnectedException
     */
    public VirtualMachine virtualMachine()
    	throws VMDisconnectedException
    {
        if(jvm == null)
            throw new VMDisconnectedException(module + " VM proxy not ready.");
        return jvm;
    }
    
    /** Connects to associated JVM.
     * 
     * @throws IllegalAttributeException
     * @throws IOException
     */
    public void connect()
    	throws IllegalAttributeException, IOException
    {
        // Attempts to attach.
        try{
            IServiceLocator locator = (IServiceLocator) Lookup
                    .serviceRegistry().locate("service locator");
            
            IObjectSpec self = this.acquireMetaObject();
            IObjectSpec connectorSpec = self.getChildSupporting(SocketAttachWrapper.class);
            SocketAttachWrapper conn = (SocketAttachWrapper)locator.incarnate(connectorSpec);
            
            jvm = conn.attach();
            
            /* We've reached a precondition - the VM is now connected and ready. */
            StdPreconditionImpl spi = new StdPreconditionImpl();
            spi.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
            
            /* Builds a resolution context from the precondition we've just stated */
            StdResolutionContextImpl srci = new StdResolutionContextImpl();
            srci.setPrecondition(spi);
            srci.setContext(this);
            
            /* Resolves all the events which were waiting for this precondition to
             * be satisfied.
             */
            queue.resolveForContext(srci);

            setDefaultRequests();
            assembleStartHandlers();
            
        }catch(IllegalConnectorArgumentsException e){
            throw new InternalError();
            // This will (or at least should) never happen.
        }catch(Exception e){
            if(jvm != null){
                try{
                    jvm.dispose();
                }catch(VMDisconnectedException ex){ }	// Just means the JVM is already dead.
            }
            throw new NestedRuntimeException(e);
        }
    }
    
    /** Determines with <b>reasonable</b> accuracy wether there's a 
     * valid connection to the associated JVM or not.
     * 
     * @return <b>true</b> if a connection exists for sure or <b>false</b>
     * if <i>perhaps</i> it doesn't exist.
     */    
    public boolean isConnected(){
        try{
            checkConnected();
            return true;
        }catch(Exception e){
            return false;
        }
    }
    
    /** Returns stringfied information about the surrogate JVM.
     * 
     */
    public String toString(){
        return "Status for Virtual Machine <" + name +">\n" +
        	   "Connected: " + isConnected() + "\n";
        	   //"Connector argument map: " + info.getAttributesByGroup("jdiconnector") + "\n";
    }
    
    /** Returns the ThreadManager for this VirtualMachine.
     * 
     * 
     * @return ThreadManager
     * @see ddproto1.debugger.managing.ThreadManager 
     */
    public IVMThreadManager getThreadManager(){
        checkConnected();
        return tm;
    }
    
    /** Returns the EventManager for this VirtualMachine.
     * 
     * @see ddproto1.debugger.eventhandler.IEventManager
     */
    public IEventManager getEventManager(){
        /* Weaker checking since the interface makes sense even if the JVM is
         * not connected.
         */
        if(handler == null){
            throw new VMDisconnectedException(
                    " Error - EventHandler is not yet ready.");
        }
        return handler;
    }
    
    public ISourceMapper getSourceMapper(){
        if(smapper == null){
            throw new VMDisconnectedException(
                    " Error - SourceMapper is not yet ready.");
        }
        return smapper;
    }
    
    public EventDispatcher getDispatcher(){
        if(disp == null)
            throw new VMDisconnectedException(" Error - EventDispatcher " +
            		"hasn't been instanciated yet.");
        return disp;
    }
    
    public IVotingManager getVotingManager(){
        return disp;
    }
 
    /** Returns the name of this node, extracted from the NodeInfo passed
     * on at the constructor.
     * 
     */
    public String getName(){
        return name;
    }
    
    public String getGID(){
        return gid;
    }
    
    public String getAttribute(String key) 
    	throws IllegalAttributeException, UninitializedAttributeException{
        
        return this.acquireMetaObject().getAttribute(key);
            
    }
    
    public void setApplicationExceptionDetector(AbstractEventProcessor aep){
        if(this.isConnected()) throw new IllegalStateException("Cannot set the abstract event processor after " +
                "the virtual machine has been launched.");
        this.aed = aep;
    }
    
    private IObjectSpec acquireMetaObject(){
        try{
            IServiceLocator locator = (IServiceLocator) Lookup.serviceRegistry()
                .locate("service locator");
        
            return locator.getMetaobject(this);
        }catch(NoSuchSymbolException ex){
            throw new NestedRuntimeException("This VirtualMachineManager has not been incarnated by a meta object.");
        }
    }
    
    private void setDefaultRequests(){
        // We want to know about all uncaught exceptions.
        ExceptionRequest er = jvm.eventRequestManager().createExceptionRequest(
                null, false, true);
        er.putProperty(DebuggerConstants.VMM_KEY, name);
        er.setSuspendPolicy(PolicyManager.getInstance().getPolicy("request.exception"));
        er.enable();
        
        // Listen to all thread start events
        ThreadStartRequest tsr = jvm.eventRequestManager().createThreadStartRequest();
        tsr.enable();
        
        // and thread death events
        ThreadDeathRequest tdr = jvm.eventRequestManager().createThreadDeathRequest();
        tdr.enable();
        
        try{
            /* We also want to stop the threads as soon as they're born. Inserting a breakpoint
             * in this method allows us to do so.
             */ 
            /* REMARK This could constitute a remarkably slow approach. It would be nice if
             * we could just come up with something better.
             */
        	DeferrableBreakpointRequest dbr = new DeferrableBreakpointRequest(name,
                DebuggerConstants.TAGGER_REG_METHOD_NAME, null);
        
        	queue.addEagerlyResolve(dbr);
            
            /*
             * Threads returning from remote calls will be trapped to this breakpoint by
             * instrumented code.
             */ 
            DeferrableBreakpointRequest tpr = new DeferrableBreakpointRequest(name,
                    DebuggerConstants.THREAD_TRAP_METHOD_NAME, null);
            
            queue.addEagerlyResolve(tpr);
            
        }catch(Exception e){
            throw new InternalError("FATAL - Failed to register thread breakpoint.", e);
        }
    }

    private void assembleStartHandlers()
    	throws Exception
    {
        final IObjectSpec self = this.acquireMetaObject();
        
        IDebugContext dc = new IDebugContext(){

            public String getProperty(String pname) {
                try{
                    return self.getAttribute(pname);
                }catch(AttributeAccessException e){
                    throw new NestedRuntimeException(e.getMessage(), e);
                }
            }

            public VirtualMachineManager getVMM() {
                return VirtualMachineManager.this;
            }

            public IVotingManager getVotingManager() {
                if(disp == null)
                    throw new IllegalStateException("Cannot retrieve voting manager.");
                return disp;
            }
            
        };
        
        // Sets context for event dispatcher
        disp.setDebugContext(dc);
        tm = new ThreadManager(dc);
        
        MessageHandler mh = MessageHandler.getInstance();
        
        /** TODO This section should definitely go to a file. */
        
        try{
            // Configures the Source Mapper for this JVM.
            IServiceLocator locator = (IServiceLocator) Lookup.serviceRegistry().locate("service locator");
            IObjectSpec mapperSpec = self.getChildSupporting(ISourceMapper.class);
            smapper = (ISourceMapper)locator.incarnate(mapperSpec);

            /* Now the event processors - almost everything that gets done
             * by the debugger gets done at the event processor level. 
             */ 
            
            // This guy will retry deferred requests
            AbstractEventProcessor dee = new DeferredEventExecutor(queue);
            dee.setDebugContext(dc);

            // This guy will set thread information for the source printer 
            // and whomever might get interested in it.
            AbstractEventProcessor tig = new ThreadInfoGatherer();
            
            // This one resumes client-side threads.
            AbstractEventProcessor csts = new ClientSideThreadStopper();
            
            // This one will print source code.
            AbstractEventProcessor sp = new SourcePrinter(smapper, mh.getStandardOutput());
            sp.setDebugContext(dc);
            
            // Exception handler (prints remote unhandled exceptions on-screen)
            AbstractEventProcessor eh = new ExceptionHandler();
            eh.setDebugContext(dc);
            
            // Updates the "current thread" in the thread manager. 
            AbstractEventProcessor tu = new ThreadUpdater();
            tu.setDebugContext(dc);
            
            // Clears fulfilled step requests.
            AbstractEventProcessor sc = new StepRequestClearer();
                        
            /* Forces all processing chains into which it's installed
             * to resume their execution.
             */
            AbstractEventProcessor rct = new ResumingChainTerminator();

            /* Registers the thread manager as a listener for events regarding
             * non-distributed thread births and deaths. 
             */
            handler.addEventListener(DelegatingHandler.THREAD_START_EVENT, tm);
            handler.addEventListener(DelegatingHandler.THREAD_DEATH_EVENT, tm);
            
            /* This is part of the complicated mechanism described in the 
             * documentation of the ddproto1.localagent.Tagger class. 
             * Refer to its documentation if you wish to understand the 
             * meaning of the following line: 
             */
            handler.addEventListener(DelegatingHandler.BREAKPOINT_EVENT, tm);            
                        
            /* Gathers thread information for LocatableEvents of interest. */
            handler.addEventListener(DelegatingHandler.BREAKPOINT_EVENT, tig);
            handler.addEventListener(DelegatingHandler.STEP_EVENT, tig);
            handler.addEventListener(DelegatingHandler.EXCEPTION_EVENT, tig);
            
            /* Clears fulfilled step requests. This guy should come before any processors
             * that make step requests. */
            handler.addEventListener(DelegatingHandler.STEP_EVENT, sc);
            
            /* Protocol for resuming threads should be processed just below the
             * thread information gatherer and after the step request clearer.
             */
            handler.addEventListener(DelegatingHandler.BREAKPOINT_EVENT, csts);
            handler.addEventListener(DelegatingHandler.STEP_EVENT, csts);
            
            /* Updates the current thread whenever the JVM might get interrupted */
            handler.addEventListener(DelegatingHandler.ALL, tu);
            Set<Integer>onHalt = new HashSet<Integer>();
            onHalt.add(new Integer(EventRequest.SUSPEND_ALL));
            handler.setListenerPolicyFilters(tu, onHalt);
            
            /* Retries deferred requests whenever a new class is loaded. */
            handler.addEventListener(DelegatingHandler.CLASSPREPARE_EVENT, dee);
                                
            /* Prints source code whenever a breakpoint is hit or when a
               stepping event is commanded. */
            handler.addEventListener(DelegatingHandler.BREAKPOINT_EVENT, sp);
            handler.addEventListener(DelegatingHandler.STEP_EVENT, sp);
            /* Stops the dispatcher when the JVM gets disconnected */
            handler.addEventListener(DelegatingHandler.VM_DISCONNECT_EVENT, disp);
            /* Notifies us so we can reset the event request queue. */
            handler.addEventListener(DelegatingHandler.VM_DISCONNECT_EVENT, this);
            
            /* Inserts the application exception detector before our exception
             * printer.
             */
            if(aed != null){
                handler.addEventListener(DelegatingHandler.EXCEPTION_EVENT, aed);
                aed.setDebugContext(dc);
            }
            
            /* Prints data about caught/uncaught exceptions so we know what's
               happening at the remote JVM. */
            handler.addEventListener(DelegatingHandler.EXCEPTION_EVENT, eh);
            
            
            /* If we don't insert this processor in the chains for handling
             * VMStart and VMDeath events, the VM will remain halted after 
             * dispatching them, because since the chains are empty by default,
             * there's no one there to vote for resuming.
             */
            handler.addEventListener(DelegatingHandler.VM_START_EVENT, rct);
            handler.addEventListener(DelegatingHandler.VM_DEATH_EVENT, rct);

        } catch (IllegalAttributeException e) { }

        /* This will starts the event dispatcher */
        disp.handleNext();
    }
    
    private void checkConnected() throws VMDisconnectedException{
        if(disp == null){
            throw new VMDisconnectedException(
                    " Error - Connection with remote JVM hasn't yet been established");
        }
        if(!disp.isConnected()){
            throw new VMDisconnectedException(
            	" Error - Connection with remote JVM is not available.");

        }
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#process(com.sun.jdi.event.Event)
     */
    public void process(Event e) {
        if(!(e instanceof VMDisconnectEvent)){
            throw new UnsupportedException("Processor " + module + " can't handle event of type " + e.getClass().toString()); 
        }
        
        queue.reset();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#setNext(ddproto1.debugger.eventhandler.processors.IJDIEventProcessor)
     */
    public void setNext(IJDIEventProcessor iep) {
        next = iep;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#getNext()
     */
    public IJDIEventProcessor getNext() {
        return next;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IJDIEventProcessor#enable(boolean)
     */
    public void enable(boolean status) {
        throw new UnsupportedException("enable is not supported by " + module);
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(key.equals(IConfigurationConstants.NAME_ATTRIB)){
            name = val;
            queue = new DeferrableRequestQueue(name, DeferrableRequestQueue.ALLOW_DUPLICATES);
        } else if(key.equals(IConfigurationConstants.GUID)){
            gid = val;
        }
    }
    
    public boolean isWritable() {
        return true;
    }

    /** These are hacks. */
    public void setLastStepRequest(int tuuid, Byte mode){
        stepRequests.put(tuuid, mode);
    }
    
    public Byte getLastStepRequest(int tuuid){
        return stepRequests.get(tuuid);
    }

}
