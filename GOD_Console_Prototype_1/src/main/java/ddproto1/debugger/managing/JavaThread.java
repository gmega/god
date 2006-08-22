/*
 * Created on Nov 3, 2005
 * 
 * file: IJavaThread.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;

import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.JDIMiscUtil;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;

/**
 * 
 * Wrapper for a Java thread. JDT's poorer cousin. 
 * 
 * @author giuliano
 *
 */
public class JavaThread extends JavaDebugElement implements ILocalThread{
    
	/** Move this to the debugger preferences. */
	private static final int TIMEOUT = 5000;
    private static final int SUSPEND_BACKOFF = 200;
    
    private static final IDistributedThread NIL_DT = new NilDistributedThread();
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(JavaThread.class);
    
    private static final Map<Integer, Integer> stepMap = new HashMap<Integer, Integer>();
    
    static{
        stepMap.put(StepRequest.STEP_INTO, DebugEvent.STEP_INTO);
        stepMap.put(StepRequest.STEP_OVER, DebugEvent.STEP_OVER);
        stepMap.put(StepRequest.STEP_OUT, DebugEvent.STEP_RETURN);
    }
    
    /** JDI thread delegate. */
    private ThreadReference fJDIThread; 
    
    private final AtomicBoolean fStepping = new AtomicBoolean(false);
    
    private final AtomicBoolean fSuspending = new AtomicBoolean(false);

    private final AtomicBoolean fResumedByRS = new AtomicBoolean();

    private final AtomicReference<StepHandler> fPendingHandler = new AtomicReference<StepHandler>();
    
    private volatile boolean fRunning = false;
    
    private volatile Integer fGUID = null;
    
    /** Lock that won't allow resume operations to be carried out at the same time
     * as operations that require thread suspension are being carried out. But will 
     * allow concurrent execution of the second type of operation.
     */ 
    private final ReentrantReadWriteLock fStackLock = new ReentrantReadWriteLock(false); 
    private final Lock fStackRead = fStackLock.readLock();
    private final Lock fStackWrite = fStackLock.writeLock();

    private IDistributedThread fDTParent;
    
    private VirtualMachineManager fVMManager;
    
    /** List of breakpoints that last suspended this thread. */
    private final List <IBreakpoint> fBreakpoints = new ArrayList<IBreakpoint>();
    
    public JavaThread(ThreadReference tDelegate, IJavaDebugTarget parent){
        super(parent);
        this.fJDIThread = tDelegate;
        //running = !tDelegate.isSuspended();
        fRunning = true;
        setParentDT(NIL_DT);
        setVMM();
    }

    public IStackFrame[] getStackFrames() throws DebugException {
        
        boolean locked = false;
        /** Creates wrappers for each frame. 
         * Yeah it's wasteful. I'll pool those in the near future (after I decipher
         * the weird rules that JDT adopts for pooling, which is suspect are that weird
         * because of concurrency issues.)  */
        try{
        	/** Locks so no one can resume this thread while we're looking at it's stack. */
            fStackRead.lock();
            locked = true;
            if(fRunning) // Illegal state.
                this.requestFailed("Cannot acquire stack frames from running thread.", null);

            try{
                if(fJDIThread.frameCount() == -1) return null; 
                List<StackFrame> frames = fJDIThread.frames();
                List<IStackFrame> nFrames = new ArrayList<IStackFrame>();
                for(StackFrame sf : frames)
                    nFrames.add(new JavaStackframe(this, sf));
                fStackRead.unlock();
                locked = false;
                return nFrames.toArray(new IStackFrame[nFrames.size()]);
                
            }catch(IncompatibleThreadStateException ex){
                this.requestFailed("Error while acquiring thread frames. ", ex);
                return null; // Line is never reached.
            }
            
        }finally{
            if(locked) fStackRead.unlock();
        }
    }
    
    public boolean resumeForRemoteStepInto(){
        StepHandler sh = fPendingHandler.get();
        if(sh == null) return false;
        sh.dontNotifyParent();
        resumeWithDetail(DebugEvent.UNSPECIFIED, true, false, true);
        return fResumedByRS.getAndSet(true);
    } 
    
//    public boolean resumedByRemoteStepping(){
//        return fResumedForRemoteStepping.getAndSet(false);
//    }

    public boolean hasStackFrames() throws DebugException {
        return !isRunning();
    }

    public int getPriority() throws DebugException {
        Field pField = null;
        try {
            pField = fJDIThread.referenceType().fieldByName("priority");
            if (pField == null) {
                requestFailed("Could not access thread priority field. ", null); 
            }
            Value priority = fJDIThread.getValue(pField);
            
            /** JDT people check for this, so I do it as well. */
            if (priority instanceof IntegerValue) {
                return ((IntegerValue)priority).value();
            }
            requestFailed("Thread priority is not a number.", null);
        } catch (RuntimeException e) {
            requestFailed("Error while retrieving thread priority.", e);
        }
        return -1;
    }

    public IStackFrame getTopStackFrame() throws DebugException {
        boolean locked = false;
        try{
            fStackRead.lock();
            locked = true;
            if(fRunning) return null;
            if(fJDIThread.frameCount() == -1) return null;
            return new JavaStackframe(this, fJDIThread.frame(0));
        }catch(IncompatibleThreadStateException ex){
            requestFailed("Error while acquiring top stack frame.", ex);
            return null;
        }finally{
            if(locked) fStackRead.unlock();
        }
    }

    public String getName() throws DebugException {
        return fJDIThread.name();
    }

    public IBreakpoint[] getBreakpoints() {
    	return breakpointsAsArray();
    }
    
    public void handleBreakpointHit(JavaBreakpoint jb){
        IBreakpoint jdtBreakpoint = jb.getMappedBreakpoint();
        addBreakpoint(jdtBreakpoint);
        setRunning(false);
        fireSuspendedByBreakpointEvent(jb);
    }
    
    private void fireSuspendedByBreakpointEvent(IBreakpoint bp){
        fireSuspendEvent(DebugEvent.BREAKPOINT, true, false);
        synchronized(this){
            fDTParent.hitByBreakpoint(bp, this);
        }
    }
    
    protected IBreakpoint [] breakpointsAsArray(){
        synchronized(fBreakpoints){
            return fBreakpoints.toArray(new IBreakpoint[fBreakpoints.size()]);
        }
    }
    
    protected void addBreakpoint(IBreakpoint bkp){
        synchronized(fBreakpoints){
            fBreakpoints.add(bkp);
        }
    }
    
    protected void clearBreakpoints(){
        synchronized(fBreakpoints){
            fBreakpoints.clear();
        }
    }

    public String getModelIdentifier() {
        return GODBasePlugin.getDefault().getBundle().getSymbolicName();
    }

    public boolean canResume() {
        return !isRunning();
    }

    public boolean canSuspend() {
        return isRunning();
    }

    public boolean isSuspended() {
        return !isRunning();
    }
    
    public boolean isRunning(){
        return fRunning;
    }

    public void resume() throws DebugException {
        resumeWithDetail(DebugEvent.CLIENT_REQUEST, true, true, false);
    }
    
    private void resumeWithDetail(int resumeDetail, 
            boolean fireEclipse, 
            boolean fireParent,
            boolean delayResumption){
        try{
            fStackWrite.lock();
            clearBreakpoints();
            setRunning(true);
            
            fireResumeEvent(resumeDetail, fireEclipse, fireParent);
            
            if(!delayResumption)
                fJDIThread.resume();
        }finally{
            fStackWrite.unlock();
        }
        
    }
    
    public void fireResumeEvent(int de, boolean fireEclipse, boolean fireParent){
        if(fireParent)
            fDTParent.resumed(this, de);
        if(fireEclipse)
            super.fireResumeEvent(de);
    }

    public synchronized void suspend() throws DebugException {
		if (!isRunning())
			return;

		/** Suspends any pending step requests. */
		abortPendingStepRequests(false, false);

		if (!fSuspending.compareAndSet(false, true))
            return;

		/** Asynchronous thread suspension. */
		Runnable suspension = new Runnable() {
			public void run() {
                try {
                    ThreadReference tr = getJDIThread();
                    tr.suspend();
                    int _timeout = TIMEOUT;
                    long timeout = System.currentTimeMillis() + _timeout;
                    boolean suspended = tr.isSuspended();
                    boolean interrupted = false;
                    /**
                     * If thread hasn't suspended, waits for a while until it
                     * does.
                     */
                    while (!suspended && System.currentTimeMillis() < timeout) {
                        try {
                            synchronized (this) {
                                wait(SUSPEND_BACKOFF);
                            }
                        } catch (InterruptedException ex) {
                            // Restore interrupted status.
                            Thread.currentThread().interrupt();
                            interrupted = true;
                            break;
                        }
                        suspended = tr.isSuspended();
                        if (suspended)
                            break;
                    }

                    /**
                     * Timed out, thread can't be suspended. Issue an error
                     * unless thread broke out of loop because of interruption.
                     */
                    if (!suspended) {
                        if (!interrupted)
                            logger.error("Failed to suspend thread.");
                        return;
                    }
                    setRunning(false);
                    getParentDistributedThread().suspended(JavaThread.this,
                            DebugEvent.CLIENT_REQUEST);
                    fireSuspendEvent(DebugEvent.CLIENT_REQUEST, true, true);
                } finally {
                    fSuspending.set(false);
                }
			}
		};

		Thread _suspension = new Thread(suspension);
		_suspension.start();
	}
    
    public void fireSuspendEvent(int de, boolean fireEclipse, boolean fireParent){
        if(fireParent)
            fDTParent.suspended(this, de);
        if(fireEclipse)
            super.fireSuspendEvent(de);
    }
    
   
    public void suspendedByVM(){
        this.setRunning(false);
        fDTParent.suspended(this, DebugEvent.CLIENT_REQUEST);
    }
    
    public void resumedByVM()
        throws DebugException
    {
        setRunning(true);
        ThreadReference thread = this.getJDIThread();
        
        /** We do the same semantic twisting of VirtualMachine#resume as 
         * the one we found inside JDT. Seems like the best way to avoid
         * bizarre suspend count configurations.
         * 
         */
        while (thread.suspendCount() > 1) {
            try {
                thread.resume();
            } catch (ObjectCollectedException e) {
            } catch (VMDisconnectedException e) {
                //disconnected();
            }catch (RuntimeException e) {
                setRunning(false);
                fireSuspendEvent(DebugEvent.CLIENT_REQUEST, true, true);
                this.requestFailed("Error while suspending thread.", e);                 
            }
        }
    }

    private void abortPendingStepRequests(boolean generateStepEnd, boolean generateStepEndForParent)
        throws DebugException
    {
        StepHandler sh = fPendingHandler.get();
    	if(sh != null){
            if(!generateStepEnd)
                sh.dontGenerateEclipseEvents();
            if(!generateStepEndForParent)
                sh.dontNotifyParent();
            sh.abort();
        }
    }
        
    public boolean canStepInto() {
        return this.canStep();
    }

    public boolean canStepOver() {
        return this.canStep();
    }

    public boolean canStepReturn() {
        return this.canStep();
    }
    
    private boolean canStep(){
        try{
            return (fPendingHandler.get() == null) 
                && fJDIThread.isSuspended() 
                && (this.getTopStackFrame() != null);
        }catch(DebugException ex){
            logger.error("Error while querying thread's step capabilities.", ex);
            return false;
        }
    }

    public boolean isStepping() {
        return fStepping.get();
    }
    
    private void setStepping(boolean mode){
        assert fStepping.compareAndSet(!mode, mode);
    }
    
    protected void setRunning(boolean mode){
    	this.fRunning = mode;
        if(mode == true) clearBreakpoints();
    }
    

    public void stepInto() throws DebugException {
        step(StepRequest.STEP_INTO, StepRequestSpec.fullNotification());
    }

    public void stepOver() throws DebugException {
        step(StepRequest.STEP_OVER, StepRequestSpec.fullNotification());
	}

    public void stepReturn() throws DebugException {
        step(StepRequest.STEP_OUT, StepRequestSpec.fullNotification());
	}
    
    public void step(int jdiStepMode, StepRequestSpec spec)
        throws DebugException
    {
        new StepHandler(StepRequest.STEP_LINE, 
                jdiStepMode, spec).step();
    }
    
    public void prepareForRemoteStepReturn() throws DebugException{
        StepHandler sh = fPendingHandler.get();
        if(sh == null){
            GODBasePlugin.throwDebugException("Can't prepare for step return " +
                "when there's no step in progress.");
        }
        sh.doGenerateEclipseEvents();
        sh.doNotifyParent();
    }

    public boolean canTerminate() {
		return getDebugTarget().canTerminate();
	}

    public boolean isTerminated() {
        return getDebugTarget().isTerminated(); 
    }

    public void terminate() throws DebugException {
		// TODO Abort evaluations
		getDebugTarget().terminate();
	}
    
    protected ThreadReference getJDIThread(){
        return fJDIThread;
    }
    
    protected synchronized VirtualMachineManager getVMM(){
        return fVMManager; 
    }
    
    private synchronized void setVMM(){
        fVMManager = (VirtualMachineManager)getDebugTarget().getAdapter(VirtualMachineManager.class);
    }
    /** Language-independent operations that are accessed by the Distributed Thread Manager
     * and by the distributed threads in which this thread may take part on. 
     * 
     * */
    public synchronized boolean setParentDT(IDistributedThread dt) {
        this.fDTParent = dt;
        return this.isSuspended();
    }

    public synchronized void unbindFromParentDT() {
        this.fDTParent = NIL_DT;
    }

    public void clearPendingStepRequests() 
        throws DebugException
    {
        StepHandler pending = fPendingHandler.get();
        if(pending != null)
            pending.abort();
    }

    public boolean hasPendingStepRequests() {
        ThreadReference tr = this.getJDIThread();
        VirtualMachine vm = this.getJDIThread().virtualMachine();
        List<StepRequest> srs = vm.eventRequestManager().stepRequests();
        for(StepRequest sr : srs){
            if(sr.thread().equals(tr)) 
                return true;
        }
        
        return false;
    }
    
    public synchronized IDistributedThread getParentDistributedThread() {
        return this.fDTParent;
    }
    
    public Integer getGUID(){
        return fGUID;
    }
    
    protected void setGUID(Integer guid){
        fGUID = guid;
        fireChangeEvent(DebugEvent.CONTENT);
    }

    private class StepHandler extends AbstractEventProcessor {
        
        private volatile int fGranularity;
        private volatile int fDepth;
        
        private volatile EventRequest fStepRequest;
        
        private volatile boolean fDone = false;
        private StepRequestSpec fRequestSpec;
        
        public StepHandler(int granularity, int depth, 
                StepRequestSpec notificationSpec){
            fDepth = depth;
            fGranularity = granularity;
            fRequestSpec = notificationSpec;
        }
        
        public StepHandler(int granularity, int depth){
            this(granularity, depth, StepRequestSpec.fullNotification());
        }
        
        public synchronized void step() throws DebugException{
            
            /** Takes over as pending step handler */
            addAsStepHandler();
            
            try{
                /** Places the step request. */
                placeStepRequest();			
            
                /** Register as listener to all JDI events 
                 * which carry our request as event request.
                 */
                registerAsListener();
            
                /** Declares that StepHandler will probe for 
                 * certain voting types.
                 */
                registerAsVoter();
            
                /** Resumes the underlying thread. */
                resumeUnderlyingThread();
                
            }catch(DebugException ex){
                // I don't know if this event generation policy 
                // is really appropriate.
                finalizeHandler(true, true); 
                throw ex;
            }
        }
        
        public void dontNotifyParent(){
            fRequestSpec = fRequestSpec
                    .disable(StepRequestSpec.PARENT_GENERATE_STEP_START
                            | StepRequestSpec.PARENT_GENERATE_STEP_END
                            | StepRequestSpec.UPDATE_PARENT_AT_END
                            | StepRequestSpec.UPDATE_PARENT_AT_START); 
        }
        
        public void doNotifyParent(){
            fRequestSpec = fRequestSpec
                .enable(StepRequestSpec.PARENT_GENERATE_STEP_START
                    | StepRequestSpec.PARENT_GENERATE_STEP_END
                    | StepRequestSpec.UPDATE_PARENT_AT_END
                    | StepRequestSpec.UPDATE_PARENT_AT_START); 
            
        }
        
        public void dontGenerateEclipseEvents(){
            fRequestSpec = fRequestSpec
                    .disable(StepRequestSpec.GENERATE_STEP_START
                            | StepRequestSpec.GENERATE_STEP_END);
        }
        
        public void doGenerateEclipseEvents(){
            fRequestSpec = fRequestSpec
                    .enable(StepRequestSpec.GENERATE_STEP_START
                            | StepRequestSpec.GENERATE_STEP_END);
        }
        
        private void registerAsVoter(){
            /** We're interested in knowing about this vote type. */
            getVMM().getVotingManager().declareVoterFor(IEventManager.NO_SOURCE);
        }
        
        private void addAsStepHandler()
            throws DebugException
        {
            if(!fPendingHandler.compareAndSet(null, this))
                GODBasePlugin.throwDebugException("Error - thread already has a pending step handler.");
        }
        
        private void stepComplete(boolean fireEclipse) 
            throws DebugException
        {
            IProcessingContext pc = ProcessingContextManager.getInstance().getProcessingContext();
            if(pc.getResults(IEventManager.RESUME_SET) == 0)
                setRunning(false);
            finalizeHandler(fireEclipse, fRequestSpec.generateParentStepEnd());
    	}
        
        private void finalizeHandler(boolean generateStepEnd, boolean notifyParent)
            throws DebugException
        {
            DebugException toThrow = null;
            
            // Clears the step request.
            clearStepRequest();
            
            // Unregister ourselves as listeners.
            IJavaNodeManager vmm = getVMM();
            try{
                if(fStepRequest != null)
                    vmm.getEventManager().removeEventListener(fStepRequest, this);
            }catch(Throwable t){
                logger.error("Error while cleaning up step handler", t);
            }
            
            JavaThread.this.setStepping(false);
            
            fDone = true;
            if(!fPendingHandler.compareAndSet(this, null)){
                toThrow = GODBasePlugin.
                    debugExceptionWithError("StepHandler mutated. This indicates an error in program logic.", 
                            null);
            }
            
            // Notify interested parties if applicable.
            fireSuspendEvent(DebugEvent.STEP_END, notifyParent, generateStepEnd);
            
            fDone = true;
            if(toThrow != null) throw toThrow;
        }
        
        private void clearStepRequest(){
            try{
                if(fStepRequest != null){
                    getVMM().virtualMachine().eventRequestManager().
                        deleteEventRequest(fStepRequest);
                }
            }catch(Throwable t){
                logger.error("Failed to remove old step requests. Further attempts to "
                                + "step this thread may result in error.");
            }
        }
        
        private void resumeUnderlyingThread() throws DebugException {
            JavaThread.this.setStepping(true);
            JavaThread.this.resumeWithDetail(
                    mapStep(fDepth), 
                    fRequestSpec.generateStepStart(), 
                    fRequestSpec.generateParentStepStart(), 
                    !fRequestSpec.shouldResume());
    	}
    
        public synchronized void specializedProcess(Event e) 
            throws DebugException
        {
            if(fDone) return; 
            EventRequest incoming = e.request();
            assert fStepRequest == incoming;
            IProcessingContext pc = ProcessingContextManager.getInstance().getProcessingContext();
            boolean noisy = true;
            if(pc != null){
                if(pc.getResults(IEventManager.NO_SOURCE) > 0)
                    noisy = false;
            }
            stepComplete(noisy & fRequestSpec.generateStepEnd());
        }
        
        protected void placeStepRequest() throws DebugException{
            VirtualMachine underVM = null;
            try{
                VirtualMachineManager vmm = getVMM();
                underVM = vmm.virtualMachine();
                StepRequest sr =
                    underVM.eventRequestManager().createStepRequest(getJDIThread(), fGranularity, fDepth);
                sr.setSuspendPolicy(PolicyManager.getInstance().getPolicy(StepRequest.class));
                
                Map <Object, Object> attributes = fRequestSpec.getPropertyMap();
                if(attributes != null){
                    for(Object key : attributes.keySet())
                        sr.putProperty(key, attributes.get(key));
                }
                
                fStepRequest = sr;
                fStepRequest.enable();

                // This line is required to feed one of the many mistakes I made in the past 
                fStepRequest.putProperty(DebuggerConstants.VMM_KEY, vmm.getName());
                
            }catch(Throwable t){
                GODBasePlugin.throwDebugExceptionWithError("Failed to set step request.", t);
            }
        }
        
        protected void registerAsListener()
            throws DebugException
        {
            // Doesn't throw exception at all.
            IJavaNodeManager vmm = getVMM(); 
            try{
                vmm.getEventManager().addEventListener(fStepRequest, this);
            }catch(Throwable t){
                GODBasePlugin.throwDebugExceptionWithError(
                        "Failed to register step handler as listener to it's own request.",t);
            }
        }
        
        protected int mapStep(int stepCode){
            return stepMap.get(stepCode);
        }
        
        public synchronized void abort() 
            throws DebugException
        {
            if(fDone) return;
    		finalizeHandler(false, false); // Step abortion is always quiet.
    	}
    }

    public boolean resumedByRemoteStepping() {
        return fResumedByRS.getAndSet(false);
    }
}
