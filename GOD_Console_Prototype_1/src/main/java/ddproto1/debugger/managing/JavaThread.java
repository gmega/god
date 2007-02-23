/*
 * Created on Nov 3, 2005
 * 
 * file: IJavaThread.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;
import ddproto1.util.DelayedResult;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * 
 * Eclipse model thread for JDI thread representations. JDT's poorer cousin. 
 * 
 * @author giuliano
 *
 */
public class JavaThread extends JavaDebugElement implements ILocalThread{
    
    public static final int RETURN_CONTROL_TO_CLIENT = 0;
        
	/** Move this to the debugger preferences. */
	private static final int TIMEOUT = 5000;
    private static final int SUSPEND_BACKOFF = 200;
    
    private static final IDistributedThread NIL_DT = new NilDistributedThread();
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(JavaThread.class);

    /** Suspend-resume and stackframe logger. */
    private static final Logger srsfLogger = MessageHandler.getInstance().getLogger(JavaThread.class.getName() + ".stackAndSuspensionLogger");
    
    private static final ConversionUtil cUtil = ConversionUtil.getInstance();

    /** Static mapping between JDI step modes and Eclipse step modes */
    private static final Map<Integer, Integer> stepMap;
    
    static{
        Map <Integer, Integer> sMap = new HashMap<Integer, Integer>();
        sMap.put(StepRequest.STEP_INTO, DebugEvent.STEP_INTO);
        sMap.put(StepRequest.STEP_OVER, DebugEvent.STEP_OVER);
        sMap.put(StepRequest.STEP_OUT, DebugEvent.STEP_RETURN);
        stepMap = Collections.unmodifiableMap(sMap);
    }
    
    /** JDI thread delegate. */
    private ThreadReference fJDIThread; 
    
    private final AtomicBoolean fStepping = new AtomicBoolean(false);
    
    private final AtomicBoolean fSuspending = new AtomicBoolean(false);

    private final AtomicBoolean fResumedByRS = new AtomicBoolean(false);

    private final AtomicReference<StepHandler> fPendingHandler = new AtomicReference<StepHandler>();
    
    private volatile boolean fRunning = false;
    
    private volatile boolean fTerminated = false;
    
    private volatile boolean fTerminating = false;
    
    private final AtomicReference<DelayedResult<Map<JavaStackframe, Integer>>> fCurrentFrames = 
        new AtomicReference<DelayedResult<Map<JavaStackframe, Integer>>>();
    private volatile IStackFrame[] fSortedFrames = null;
    
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
        
        try{
        	/** Locks so no one can resume this thread 
             * while we're looking at it's stack. */
            fStackRead.lock();

            // Okay, so it's not running.
            if(isRunning()) 
                return INodeManager.NO_CALLSTACK;
            
            while(true){
                // Check-then-act is safe, as frames can't 
                // be invalidated without a resume being issued.
                if(hasCachedFrames())
                    return getSortedFrames();
                computeAndCacheFrames();
            }
                                   
        }finally{
            fStackRead.unlock();
        }
    }
    
    /* -------------------------------------------------------------
       --- Careful with these methods, they aren't synchronized. ---
       ------------------------------------------------------------- */
    private boolean hasCachedFrames(){
        DelayedResult dr = fCurrentFrames.get();
        if(fCurrentFrames.get() == null) return false;
        try{
            dr.get();
            return true;
        }catch(Exception ex){ return false; }
    }
    
    private void computeAndCacheFrames()
        throws DebugException
    {
        DelayedResult<Map<JavaStackframe, Integer>> dr = new DelayedResult<Map<JavaStackframe, Integer>>();
        
        if(!fCurrentFrames.compareAndSet(null, dr))
            return;
        
        if(srsfLogger.isDebugEnabled())
            srsfLogger.debug("Getting frames from thread " + getNameSafe());

        Map<JavaStackframe, Integer> frameCache = new HashMap<JavaStackframe, Integer>();
        try{
            int jdiFrameCount = fJDIThread.frameCount();
            if(jdiFrameCount == -1) return;

            IStackFrame[] sortedCache = new IStackFrame[jdiFrameCount];

            for(int i = 0; i < jdiFrameCount; i++){
                JavaStackframe frame = new JavaStackframe(this);
                frameCache.put(frame, i);
                sortedCache[i] = frame;
            }
            
            if(srsfLogger.isDebugEnabled())
                srsfLogger.debug("Got " + frameCache.size() + " frames from thread " + getNameSafe());
            
            assert fCurrentFrames.get() == dr;

            fSortedFrames = sortedCache;
            dr.set(Collections.unmodifiableMap(frameCache));

        }catch(IncompatibleThreadStateException ex){
            dr.setException(ex);
            fCurrentFrames.set(null);
            
            this.requestFailed("Error while acquiring thread frames. ", ex);
        }catch(VMDisconnectedException ex){
            dr.setException(ex);
            fCurrentFrames.set(null);
            
            if(terminationWarnIssued()){
                return;
            }else{
                this.requestFailed("Error while acquiring thread frames. ", ex);
            }
        }
    }    
    
    private IStackFrame[] getSortedFrames(){
        IStackFrame [] sortedFrames = fSortedFrames;
        if(sortedFrames == null) return new IStackFrame[0];
        IStackFrame [] sortedFramesCopy = new IStackFrame[sortedFrames.length];
        System.arraycopy(sortedFrames, 0, sortedFramesCopy, 0, sortedFrames.length);
        return sortedFramesCopy;
    }
    
    private void invalidateStackframes(){
        DelayedResult <Map<JavaStackframe, Integer>>dr = fCurrentFrames.get();
        if(dr == null) return; // No cached frames to invalidate.
        
        try{
            // dr.get() may block if computeAndCacheFrames 
            // hasn't yet completed, but that's okay, as 
            // we don't expect computeAndCacheFrames to block
            // indefinitely. Also, if computation of cached
            // frames fail, we'll get a side-effect exception,
            // which just means there's nothing to invalidate, 
            // as the computation that would lead to the frames
            // we have to invalidate just failed.
            for(JavaStackframe frame : dr.get().keySet()){
                frame.invalidate();
            }
            fCurrentFrames.set(null);
        }catch(Exception ex){ 
            // Nothing to do.
        }
    }
    
    /* -------------------------------------------------------------
       ------------------------------------------------------------- */
    
    /**
     *  
     */
    protected StackFrame getJDIStackFrame(JavaStackframe sFrame)
    {
        Map<JavaStackframe, Integer> localCopy = null;
        try{
            localCopy = fCurrentFrames.get().get();
        }catch(Exception ex){ 
            // Nothing to do. If we got an exception,
            // the assignment of localCopy didn't complete
            // and we'll return null.
        }
        
        if(localCopy == null)
            return null;
        Integer frameIndex = localCopy.get(sFrame);
        if(frameIndex == null) return null;
        try{
            return getJDIThread().frame(frameIndex);
        }catch(IncompatibleThreadStateException ex){
            return null;
        }
    }
    
    /** 
     * This will cause the JavaThread to prepare for termination.
     * In termination mode, the JavaThread will swallow certain types
     * of exceptions, like VMDisconnectedExceptions.
     */
    public void issueTerminationWarning(){
        fTerminating = true;
    }
    
    private boolean terminationWarnIssued(){
        return fTerminating;
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
            IStackFrame[] frames = getStackFrames();
            if(frames.length != 0) return frames[0];
            return null;
        }finally{
            if(locked) fStackRead.unlock();
        }
    }

    public String getName() throws DebugException {
        try{
            return getJDIThread().name();
        }catch(ObjectCollectedException ex){
            return "<has been garbage collected>";
        }catch(VMDisconnectedException ex){
            return "<disconnected>";
        }
    }
    
    private String getNameSafe(){
        try{
            return getJDIThread().name();
        }catch(Exception ex){
            return "Error";
        }
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
        return fRunning && !isTerminated();
    }

    public void resume() throws DebugException {
        if(!isSuspended()) return;
        resumeWithDetail(DebugEvent.CLIENT_REQUEST, true, true, false);
    }
    
    private void resumeWithDetail(int resumeDetail, 
            boolean fireEclipse, 
            boolean fireParent,
            boolean delayResumption){
        try{
            fStackWrite.lock();
            clearBreakpoints();
            invalidateStackframes();
            
            setRunning(true);
            
            fireResumeEvent(resumeDetail, fireEclipse, fireParent);
            
            if(!delayResumption)
                fJDIThread.resume();
        }finally{
            fStackWrite.unlock();
        }
        
    }
    
    public void fireResumeEvent(int de, boolean fireEclipse, boolean fireParent){
        if(srsfLogger.isDebugEnabled()){
            srsfLogger.debug("Resuming thread " + getNameSafe() + " with opts "
                    + (fireEclipse?"fireEclipse":"") + (fireParent?"fireParent":""));
        }

        if(fireEclipse)
            super.fireResumeEvent(de);
        if(fireParent)
            fDTParent.resumed(this, de);
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
        if(srsfLogger.isDebugEnabled()){
            srsfLogger.debug("Thread " + getNameSafe() + " suspended.");
        }

        if(fireEclipse)
            super.fireSuspendEvent(de);
        if(fireParent)
            fDTParent.suspended(this, de);

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
                && !isTerminated()
                && !isRunning()
                && (this.getTopStackFrame() != null);
        }catch(DebugException ex){
            logger.error("Error while querying thread's step capabilities.", ex);
            return false;
        }
    }

    public boolean isStepping() {
        return fStepping.get();
    }
    
    private boolean setStepping(boolean mode){
        return fStepping.compareAndSet(!mode, mode);
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
    
    public void handleDeath(){
        setRunning(false);
        setTerminated(true);
        getParentDistributedThread().died(this);
        fireTerminateEvent();
    }
    
    private void setTerminated(boolean value){
        fTerminated = value;
    }

    public boolean canTerminate() {
		return !fTerminated;
	}

    public boolean isTerminated() {
        return fTerminated; 
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
        
        private final AtomicBoolean fDone = new AtomicBoolean(false);
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
            if(pc.getResults(IEventManager.RESUME_SET) == 0){
                setRunning(false);
            }
            finalizeHandler(fireEclipse, fRequestSpec.generateParentStepEnd());
    	}
        
        private void finalizeHandler(boolean generateStepEnd, boolean notifyParent)
            throws DebugException
        {
            fDone.set(true);
            
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
            
            if(!fPendingHandler.compareAndSet(this, null)){
                toThrow = GODBasePlugin.
                    debugExceptionWithError("StepHandler mutated. This indicates an error in program logic.", 
                            null);
            }
            
            // Notify interested parties if applicable.
            fireSuspendEvent(DebugEvent.STEP_END, notifyParent, generateStepEnd);
            
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
            if(!JavaThread.this.setStepping(true))
                GODBasePlugin.throwDebugException("Inconsistent state - thread already in step mode.");
            JavaThread.this.resumeWithDetail(
                    mapStep(fDepth), 
                    fRequestSpec.generateStepStart(), 
                    fRequestSpec.generateParentStepStart(), 
                    !fRequestSpec.shouldResume());
    	}
    
        public void specializedProcess(Event e) 
            throws DebugException
        {
            IProcessingContext pc = ProcessingContextManager.getInstance().getProcessingContext();
            /** Step request has been aborted, vote for resumption. */
            if(!fDone.compareAndSet(false, true)){
                pc.vote(IEventManager.RESUME_SET);
                abort();
                return;
            }
            
            EventRequest incoming = e.request();
            assert fStepRequest == incoming;
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
        
        public void abort() 
            throws DebugException
        {
            if(!fDone.compareAndSet(false, true)) return;
            // Aborted steps don't generate suspend notifications.
    		finalizeHandler(false, false);
            // Generate resumption event (thread no longer stepping).
            fireResumeEvent(DebugEvent.CLIENT_REQUEST, true, false);
        }
    }

    public boolean resumedByRemoteStepping() {
        return fResumedByRS.getAndSet(false);
    }
    
    public String toString(){
        StringBuffer sBuffer = new StringBuffer();
        Integer guid = getGUID();
        if(guid != null){
            sBuffer.append("[guid: ");
            sBuffer.append(cUtil.uuid2Dotted(guid));
            sBuffer.append("] - ");
        }
        
        sBuffer.append(getJDIThread().toString());
        return sBuffer.toString();
    }
}
