/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: VirtualMachineManager.java
 */

package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;
import com.sun.jdi.request.VMDeathRequest;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.DelegatingHandler;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IVotingManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.eventhandler.processors.ClientSideThreadStopper;
import ddproto1.debugger.eventhandler.processors.ClassPrepareNotifier;
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
import ddproto1.exception.TargetRequestFailedException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.sourcemapper.ISourceMapper;
import ddproto1.util.DelayedResult;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.JDIEventProcessorTrait;
import ddproto1.util.traits.JDIEventProcessorTrait.JDIEventProcessorTraitImplementor;


/**
 * This class groups the infrastructure required to manage
 * events and communication with an individual JVM. It also
 * acts as a façade for placing requests.
 * 
 * Responsible for assembling Dispatchers, Handlers and IJDIEventProcessor
 * chains.
 * 
 * This class should be thread-safe, but it's not. As it is, this class is  
 * a recipe for disaster. There is a lot of legacy code there. I must merge
 * this class and VMMDebugTargetImpl, possibly rewritting both of them. Will
 * do it when there's time. 
 * 
 * @author giuliano
 *
 */
public class VirtualMachineManager implements JDIEventProcessorTraitImplementor, IJavaNodeManager{
    
	private static final int INITIAL = 0;
	private static final int CONNECTING = 1;
	private static final int CONNECTED = 2;
	private static final int DISCONNECTING = 3;
	private static final int DISCONNECTED = 4;
	private static final int TERMINATING = 5;
	private static final int TERMINATED = 6;
    
    private static final String[] stateNames = { "INITIAL", "CONNECTING",
            "CONNECTED", "DISCONNECTING", "DISCONNECTED", "TERMINATING",
            "TERMINATED" };
	
    private static final String module = "VirtualMachineManager -";
    private static final Logger logger = MessageHandler.getInstance().getLogger(VirtualMachineManager.class);
    
    private static final int JVM_EXIT_CODE = 1;

    private DeferrableRequestQueue queue;
    private EventDispatcher disp = null;
    private DelegatingHandler handler;
    private ISourceMapper smapper;
    private VirtualMachine jvm;
    private ThreadManager tm;
    private AbstractEventProcessor aed;
    
    private IJDIEventProcessor _thisProcessor;
    private IJDIEventProcessor next;
   
    private volatile VMMDebugTargetImpl target;
    
    private IJDIConnector conn;
    
    private String name;
    private String gid;
    
    private volatile IProcess process;
    
    private boolean suspended = true;
    
    private int state = INITIAL;
    
    /** This has to do with the event processor, not
     * with the VirtualMachineManager state flags.
     */
    private boolean isEnabled = true;
    
    /** Creates a new VirtualMachineManager.
     * 
     * @param info Virtual machine specification.
     * 
     */
    protected VirtualMachineManager() 
    	throws AttributeAccessException
    {
        synchronized(this){
            setHandler(new DelegatingHandler());
            setDisp(new EventDispatcher(handler));
            set_thisProcessor(new JDIEventProcessorTrait(this));
        }
        /*TODO When code hits beta, enable the disallow duplicates. */
        //queue = new DeferrableRequestQueue(name, DeferrableRequestQueue.DISALLOW_DUPLICATES);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getDeferrableRequestQueue()
     */
    public synchronized DeferrableRequestQueue getDeferrableRequestQueue(){
        return queue;
    }
    
    private synchronized void setDeferrableRequestQueue(DeferrableRequestQueue drq){
        this.queue = drq;
    }
    
    /** Returns the associated JVM mirror.
     * 
     * @return
     * @throws VMDisconnectedException
     */
    public synchronized VirtualMachine virtualMachine()
    		throws VMDisconnectedException
    {
        if(jvm == null)
            throw new VMDisconnectedException(module + " VM proxy not ready.");
        return jvm;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#prepareForConnecting()
     */
    public void prepareForConnecting() throws DebugException{
        // Attempts to prepare for connection.
        try{
            synchronized(this){
                if(isConnected() || isDisconnecting() || isTerminating() || isTerminated()){
                    throw new IllegalStateException("Target is not in a valid state.");
                }
            }
                
            IServiceLocator locator = (IServiceLocator) Lookup
                    .serviceRegistry().locate("service locator");
            
            IObjectSpec self = this.acquirePhantomObject();
            IObjectSpec connectorSpec = self.getChildSupporting(IJDIConnector.class);
            setConn((IJDIConnector)locator.incarnate(connectorSpec));
            getConn().prepare();
            
        }catch(Throwable t){
            GODBasePlugin.throwDebugExceptionWithError("Error while preparing for connection.", t);
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#connect()
     */
    public void connect()
    		throws IllegalAttributeException, DebugException
    {
        if(getConn() == null)
            throw new IllegalStateException("You must call prepareForConnecting before calling connect.");
    
        if(getProcess() == null)
            throw new IllegalStateException("You must associate a process to this proxy before attempting to connect.");
        
        Throwable thrown = null;
        
        // Attempts to attach.
        try{
            /** For the target to be eligible for conection,
             * it cannot be connected, disconnecting, terminating nor
             * terminated. The only viable state is stopped.
             */
            synchronized(this){
                if(isConnected() || isDisconnecting() || isTerminating() || isTerminated() || isConnecting()){
                    throw new IllegalStateException("Target is not in a valid state.");
                }
                this.connecting();
            }

            /** We now create the debug target. */
            VMMDebugTargetImpl dti = 
            		new VMMDebugTargetImpl(process.getLaunch(), true, false, process, true, this);

            assembleStartHandlers();
            setJvm(getConn().connect());
            setDefaultRequests();
            
            connected();
            target = dti;

            /* This will start the event dispatcher */
            disp.handleNext();

        }catch(Throwable t){
            thrown = t;
            GODBasePlugin.throwDebugExceptionWithError("Error while connecting to remote target", t);
        }finally{
            if(thrown != null)
                cleanUp();
        }
    }
    
    private void cleanUp(){
        try{
            DeferrableRequestQueue drq =
                getDeferrableRequestQueue();
            if(drq != null) drq.reset();
        }catch(Throwable t){
            logger.error("Error resetting the deferrable request queue.", t);
        }
        
        try{
            VirtualMachine vm = virtualMachine();
            if(vm != null) vm.exit(DebuggerConstants.UNKNOWN);
        }catch(Throwable t){
            logger.error("Failed to shut down the remote virtual machine.", t);
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#terminate()
     */
    public void terminate(){
        synchronized(this){
            if(!isAvailable()) return;
            this.terminating();
        }
        try{
            virtualMachine().exit(JVM_EXIT_CODE);
        }catch(VMDisconnectedException ex){
            /** JVM has been disconnected. */
            disconnected();
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#disconnect()
     */
    public void disconnect(){
        synchronized(this){
            if(!isAvailable()) return;
            this.disconnecting();
        }
        try{
            virtualMachine().dispose();
        }catch(VMDisconnectedException ex){
            /** Already disconnected. */
            disconnected();
        }
    }
    
    /**
     * Initial state means that the machine hasn't connected yet. 
     * 
     * @return
     */
    private synchronized boolean isAtInitialState(){
    		return state == INITIAL;
    }
    
    /** Returns stringfied information about the surrogate JVM.
     * 
     */
    public String toString(){
        return "Status for Virtual Machine <" + name +">\n" +
        	   "Connected: " + isConnected() + "\n";
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getThreadManager()
     */
    public IJavaThreadManager getThreadManager(){
        checkConnected();
        return tm;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getEventManager()
     */
    public IEventManager getEventManager(){
        return handler;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getSourceMapper()
     */
    public ISourceMapper getSourceMapper(){
        if(smapper == null){
            throw new VMDisconnectedException(
                    " Error - SourceMapper is not yet ready.");
        }
        return smapper;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getDispatcher()
     */
    public EventDispatcher getDispatcher(){
        return disp;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getVotingManager()
     */
    public IVotingManager getVotingManager(){
        return disp;
    }
 
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getName()
     */
    public String getName(){
        return name;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#getGID()
     */
    public String getGID(){
        return gid;
    }
    
    public String getAttribute(String key) 
    	throws IllegalAttributeException, UninitializedAttributeException{
        
        return this.acquirePhantomObject().getAttribute(key);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#setApplicationExceptionDetector(ddproto1.debugger.eventhandler.processors.AbstractEventProcessor)
     */
    public void setApplicationExceptionDetector(AbstractEventProcessor aep){
        if(this.isConnected()) throw new IllegalStateException("Cannot set the abstract event processor after " +
                "the virtual machine has been launched.");
        this.aed = aep;
    }
    
    private IObjectSpec acquirePhantomObject(){
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
        // FIX: I don't think this is necessary anymore, since the app
        // will die and we'll dump the stuff on-screen.
//        ExceptionRequest er = jvm.eventRequestManager().createExceptionRequest(
//                null, false, true);
//        er.putProperty(DebuggerConstants.VMM_KEY, name);
//        er.setSuspendPolicy(PolicyManager.getInstance().getPolicy("request.exception"));
//        er.enable();
        
        // Listen to all thread start events
        ThreadStartRequest tsr = jvm.eventRequestManager().createThreadStartRequest();
        tsr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        tsr.enable();
        
        // and thread death events
        ThreadDeathRequest tdr = jvm.eventRequestManager().createThreadDeathRequest();
        tdr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        tdr.enable();
        
        // listen to vm death
// Unneeded. VM already produces a vm death event without requests.
//        VMDeathRequest vmd = jvm.eventRequestManager().createVMDeathRequest();
//        vmd.addCountFilter(1);
//        vmd.enable();
        
        try{
            /* We also want to stop the threads as soon as they're born. Inserting a breakpoint
             * in this method allows us to do so.
             */ 
            /* REMARK This could constitute a remarkably slow approach. It would be nice if
             * we could just come up with something better.
             */
        	DeferrableBreakpointRequest dbr = new DeferrableBreakpointRequest(name,
                DebuggerConstants.TAGGER_REG_METHOD_NAME, null);
        
        	getDeferrableRequestQueue().addEagerlyResolve(dbr);
            
            /*
             * Threads returning from remote calls will be trapped to this breakpoint by
             * instrumented code.
             */ 
            DeferrableBreakpointRequest tpr = new DeferrableBreakpointRequest(name,
                    DebuggerConstants.THREAD_TRAP_METHOD_NAME, null);
            
            getDeferrableRequestQueue().addEagerlyResolve(tpr);
            
        }catch(Exception e){
            throw new InternalError("FATAL - Failed to register thread breakpoint.", e);
        }
    }

    private void assembleStartHandlers()
    		throws Exception
    {
        final IObjectSpec self = this.acquirePhantomObject();
        
        IDebugContext dc = new IDebugContext(){

            public String getProperty(String pname) {
                try{
                    return self.getAttribute(pname);
                }catch(AttributeAccessException e){
                    // This sucks - it doesn't have to be an
                    // unchecked exception.
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
            AbstractEventProcessor dee = new ClassPrepareNotifier(getDeferrableRequestQueue());
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
            handler.removeEventListener(DelegatingHandler.VM_DEATH_EVENT, tu);
            Set<Integer>onHalt = new HashSet<Integer>();
            onHalt.add(new Integer(EventRequest.SUSPEND_ALL));
            handler.setListenerPolicyFilters(tu, onHalt);
            
            /* Retries deferred requests whenever a new class is loaded. */
            handler.addEventListener(DelegatingHandler.CLASSPREPARE_EVENT, dee);
                                
            /* Prints source code whenever a breakpoint is hit or when a
               stepping event is commanded. */
            handler.addEventListener(DelegatingHandler.BREAKPOINT_EVENT, sp);
            handler.addEventListener(DelegatingHandler.STEP_EVENT, sp);

            /* Notifies us so we can reset the event request queue. */
            handler.addEventListener(DelegatingHandler.VM_DISCONNECT_EVENT, _thisProcessor);
            handler.addEventListener(DelegatingHandler.VM_DEATH_EVENT, _thisProcessor);
            
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
            
            
            /** Insert the thread manager as a listener for VMStartEvent */
            handler.addEventListener(DelegatingHandler.VM_START_EVENT, tm);
            handler.addEventListener(DelegatingHandler.VM_DEATH_EVENT, tm);
            handler.addEventListener(DelegatingHandler.VM_DISCONNECT_EVENT, tm);
            
            /* If we don't insert this processor in the chains for handling
             * VMStart and VMDeath events, the VM will remain halted after 
             * dispatching them, because since the chains are empty by default,
             * there's no one there to vote for resuming.
             */
            handler.addEventListener(DelegatingHandler.VM_START_EVENT, rct);
            handler.addEventListener(DelegatingHandler.VM_DEATH_EVENT, rct);

        } catch (Throwable t) { 
            logger.error("Error while setting event handlers! Debugger may not operate correctly.", t);
        }
    }
    
    private void checkConnected() throws VMDisconnectedException{
        if(!disp.isConnected()){
            throw new VMDisconnectedException(
            	" Error - Connection with remote JVM is not available.");

        }
    }

    public void specializedProcess(Event e) {
        if(e instanceof VMDisconnectEvent){
        	if(isTerminating()) terminated();
        	else disconnected();
        }else if(e instanceof VMDeathEvent){
            terminated();
        }else{
            throw new UnsupportedException("Processor " + module + " can't handle event of type " + e.getClass().toString());
        }
        
        getDeferrableRequestQueue().reset();
    }

    public void next(IJDIEventProcessor iep) { next = iep; }
    
    public IJDIEventProcessor next() { return next; }

    public void enabled(boolean status) { this.isEnabled = status; }
    
    public boolean enabled() { return isEnabled; }
    

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(key.equals(IConfigurationConstants.NAME_ATTRIB)){
            name = val;
            setDeferrableRequestQueue(new DeferrableRequestQueue(name,
                    DeferrableRequestQueue.ALLOW_DUPLICATES));
        } else if(key.equals(IConfigurationConstants.GUID_ATTRIBUTE)){
            gid = val;
        }
    }
    
    /* (non-Javadoc)
	 * @see ddproto1.debugger.managing.IJavaNodeManager#isAvailable()
	 */
	public synchronized boolean isAvailable(){
	    return !isTerminated() & !isTerminating() & !isDisconnecting() & isConnected(); 
	}

	public Object getAdapter(Class adapter) {
        if(adapter.isAssignableFrom(VirtualMachineManager.class))
            return this;
        /** This might be null if the machine hasn't yet connected. */
        else if(IDebugTarget.class.isAssignableFrom(adapter))
            return target;
        return null;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#suspend()
     */
    public void suspend() throws TargetRequestFailedException{
        if(!isAvailable()) 
            throw new IllegalStateException("Virtual machine is unavailable for suspension.");
        
        try{
            virtualMachine().suspend();
            suspended(true);
            tm.notifyVMSuspend();
            fireSuspended(DebugEvent.CLIENT_REQUEST);
        }catch(Exception ex){
            logger.error("Failed to suspend virtual machine.");
            suspended(false);
            fireResumed(DebugEvent.CLIENT_REQUEST);
            throw new TargetRequestFailedException(ex);
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.IJavaNodeManager#resume()
     */
    public void resume() throws TargetRequestFailedException{
        if(!isSuspended())
            throw new IllegalStateException("Virtual machine is unavailable for resuming.");
        this.resumeInternal();
    }
    
    protected void resumeInternal() throws TargetRequestFailedException{
    		if(!isConnected() || isTerminated() || isTerminating())
    			throw new TargetRequestFailedException("VM is unavailable");
        try{
            virtualMachine().resume();
            tm.notifyVMResume();
            fireResumed(DebugEvent.CLIENT_REQUEST);
        }catch(Exception ex){
            logger.error("Failed to resume virtual machine.");
            suspended(true);
            fireSuspended(DebugEvent.CLIENT_REQUEST);
            throw new TargetRequestFailedException(ex);
        }
    }
    
    protected void fireSuspended(int detail){
        if(target != null) target.handleSuspend(detail);
    }
    
    protected void fireResumed(int detail){
        if(target != null) target.handleResume(detail);
    }
    
    protected void fireDisconnected(){
        if(target != null) target.handleDisconnect();
    }
    
    protected void fireTerminated(){
        if(target != null) target.handleDeath();
    }

	public synchronized IJavaDebugTarget getDebugTarget()
		throws IllegalStateException
	{
		if(target == null)
			throw new IllegalStateException("Debug target not yet available");
		
		return target;
	}
    
	public boolean connectsBack() { return true; }
	
	public boolean isWritable() { return true; }

	public synchronized void setProcess (IProcess process){
        if(isConnected() || isDisconnecting() || isTerminating() || isTerminated()){
            throw new IllegalStateException("Cannot set process after connection.");
        }
        this.process = process;
    }
    
    public synchronized IProcess getProcess(){
        if(process == null)
            throw new IllegalStateException("Process hasn't been set.");
        return process;
    }
	
	/*******************************************************************************
	 * Below this line are methods used for manipulating the state of the virtual  *
	 * machine manager.                                                            *
	 *                                                                             *
	 * We have the following states:                                               *
	 *                                                                             *
	 * INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED                 *
	 *                                                                             *
	 * A machine that is at the CONNECTED state can also be:                       *
	 *                                                                             *
	 * SUSPENDED, RUNNING                                                          *
	 *                                                                             *
	 * As for the five initial states, the following transitions are valid         *
	 * transitions:                                                                *
	 *                                                                             *
	 * INITIAL -> CONNECTING                                                       * 
	 *                                                                             *                                                                            *
	 * CONNECTING -> CONNECTED                                                     *  
	 * CONNECTING -> DISCONNECTED                                                  *
	 * CONNECTING -> TERMINATED                                                    *
	 *                                                                             *
	 * CONNECTED -> TERMINATING                                                    *
	 * CONNECTED -> DISCONNECTING                                                  *
	 *                                                                             *
	 * TERMINATING -> TERMINATED                                                   *
	 *                                                                             *
	 *******************************************************************************/

	public boolean isSuspended(){ return suspended; }

	public synchronized boolean isTerminating() { return state == TERMINATING; }

	public synchronized boolean isDisconnecting() { return state == DISCONNECTING; }

	public synchronized boolean isTerminated() { return state == TERMINATED; }
	
	public synchronized boolean isConnecting() { return state == CONNECTING; }

	public synchronized boolean isConnected() { return state == CONNECTED; }

	/** Actual transition functions */
	private synchronized void connecting(){
		performTransition(isAtInitialState(), CONNECTING);
	}
	
	private synchronized void disconnecting(){ 
        performTransition(isConnected(), DISCONNECTING);
	}
    
	private synchronized void terminating(){
        performTransition(isConnected(), TERMINATING);
	}

	private synchronized void disconnected(){
        performTransition(isConnecting() || isDisconnecting(), DISCONNECTED);
	}

	private synchronized void terminated(){
        performTransition(isTerminating() || isConnecting() || isConnected(), TERMINATED);
	    if(target != null) target.handleDeath();
	    fireTerminated();
	}
	
	private synchronized void suspended(boolean stats) {
		if(!isConnected() || !suspended)
            throw new IllegalStateException(
                    "Can't transition from state (" 
                    + stateAsString(state) + "," + suspended +") to suspension " + stats);
	    suspended = stats;
	}
    
    private synchronized void connected(){
        performTransition(isConnecting(), CONNECTED);
        
        /* We've reached a precondition - the VM is now connected and ready. */
        StdPreconditionImpl spi = new StdPreconditionImpl();
        spi.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        
        /* Builds a resolution context from the precondition we've just stated */
        StdResolutionContextImpl srci = new StdResolutionContextImpl();
        srci.setPrecondition(spi);
        srci.setContext(this);
        
        try{
            /* Resolves all the events which were waiting for this precondition to
             * be satisfied.
             */
            queue.resolveForContext(srci);
        }catch(Exception ex){ }
    }
    
    private void performTransition(boolean condition, int newState){
        if(condition){
            state = newState;
            return;
        }
        throw new IllegalStateException("Invalid transition: " + stateAsString(state) + "->" + stateAsString(newState)); 
    }
    
    private String stateAsString(int stateIdx){
        return (stateIdx >= stateNames.length)?"unknown":stateNames[stateIdx];
    }

    private synchronized IJDIConnector getConn() {
        return conn;
    }

    private synchronized void setConn(IJDIConnector conn) {
        this.conn = conn;
    }
    
    private synchronized void setHandler(DelegatingHandler handler) {
        this.handler = handler;
    }

    private synchronized void setDisp(EventDispatcher disp) {
        this.disp = disp;
    }

    private synchronized void setJvm(VirtualMachine jvm) {
        this.jvm = jvm;
    }

    private synchronized void set_thisProcessor(IJDIEventProcessor processor) {
        _thisProcessor = processor;
    }

    public IBreakpoint setBreakpointFromEvent(ddproto1.util.commons.Event evt) throws DebugException{
        
        try{
            String brLine = evt.getAttribute("lin");
            String clsName = evt.getAttribute("cls");
            String ltuid = evt.getAttribute("ltid");
        
            JavaBreakpoint bkp = new JavaBreakpoint(clsName, Integer.parseInt(brLine), null);

            /** This is really great. 
             * The two lines of code that follow ensure that:
             *         
             * 1) This breakpoint will only halt the correct thread.
             * 2) This breakpoint will remove itself after serving its purpose,
             *    without affecting other threads or other user breakpoints. 
             */
            final List<Integer> tFilters = new ArrayList<Integer>(1);
            tFilters.add(0, new Integer(ltuid));

            bkp.addToTarget(this.getDebugTarget(),
                    new JavaBreakpoint.IFilterProvider(){
                        public List<Integer> getThreadFilters() {
                            return tFilters;
                        }

                        public boolean isOneShot() {
                            return true;
                        }
            });
            
            return bkp;
        }catch(Exception ex){
            GODBasePlugin.throwDebugExceptionWithError("Failed to set breakpoint from event.", ex);
            return null;
        }
    }
}
