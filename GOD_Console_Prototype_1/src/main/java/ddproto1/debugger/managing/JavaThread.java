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
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.IResumeSuspendEventListener;
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
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(JavaThread.class);
    
    private static final Map<Integer, Integer> stepMap = new HashMap<Integer, Integer>();
    static{
        stepMap.put(StepRequest.STEP_INTO, DebugEvent.STEP_INTO);
        stepMap.put(StepRequest.STEP_OVER, DebugEvent.STEP_OVER);
        stepMap.put(StepRequest.STEP_OUT, DebugEvent.STEP_RETURN);
    }
    
    /** JDI thread delegate. */
    private ThreadReference fJDIThread; 
    
    private final AtomicBoolean isStepping = new AtomicBoolean(false);
    
    private volatile boolean isSuspending = false;
    
    private volatile boolean running = false;
    
    /** Lock that won't allow resume operations to be carried out at the same time
     * as operations that require thread suspension are being carried out. But will 
     * allow concurrent execution of the second type of operation.
     */ 
    private final ReentrantReadWriteLock stackLock = new ReentrantReadWriteLock(false); 
    private final Lock readLock = stackLock.readLock();
    private final Lock writeLock = stackLock.writeLock();
    
    /** Pending step request handler (null if none) */
    StepHandler pendingHandler;
    
    private IDistributedThread parent;
    
    private VirtualMachineManager fVMManager;
    
    /** List of breakpoints that last suspended this thread. */
    private final List <IBreakpoint> sBreakpoints = new ArrayList<IBreakpoint>();
    
    public JavaThread(ThreadReference tDelegate, IJavaDebugTarget parent){
        super(parent);
        this.fJDIThread = tDelegate;
        //running = !tDelegate.isSuspended();
        running = true;
        setParentDT(new NilDistributedThread());
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
            readLock.lock();
            locked = true;
            if(running) // Illegal state.
                this.requestFailed("Cannot acquire stack frames from running thread.", null);

            try{
                if(fJDIThread.frameCount() == -1) return null; 
                List<StackFrame> frames = fJDIThread.frames();
                List<IStackFrame> nFrames = new ArrayList<IStackFrame>();
                for(StackFrame sf : frames)
                    nFrames.add(new JavaStackframe(this, sf));
                readLock.unlock();
                locked = false;
                return nFrames.toArray(new IStackFrame[nFrames.size()]);
                
            }catch(IncompatibleThreadStateException ex){
                this.requestFailed("Error while acquiring thread frames. ", ex);
                return null; // Line is never reached.
            }
            
        }finally{
            if(locked) readLock.unlock();
        }
    }

    public boolean hasStackFrames() throws DebugException {
//   		System.out.println("Thread " + this.tDelegate.name() + " is " + (running?"running":"not running"));
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
            readLock.lock();
            locked = true;
            if(running) return null;
            if(fJDIThread.frameCount() == -1) return null;
            return new JavaStackframe(this, fJDIThread.frame(0));
        }catch(IncompatibleThreadStateException ex){
            requestFailed("Error while acquiring top stack frame.", ex);
            return null;
        }finally{
            if(locked) readLock.unlock();
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
        super.fireSuspendEvent(DebugEvent.BREAKPOINT);
        synchronized(this){
            parent.hitByBreakpoint(bp, this);
        }
    }
    
    protected IBreakpoint [] breakpointsAsArray(){
        synchronized(sBreakpoints){
            return sBreakpoints.toArray(new IBreakpoint[sBreakpoints.size()]);
        }
    }
    
    protected void addBreakpoint(IBreakpoint bkp){
        synchronized(sBreakpoints){
            sBreakpoints.add(bkp);
        }
    }
    
    protected void clearBreakpoints(){
        synchronized(sBreakpoints){
            sBreakpoints.clear();
        }
    }

    public String getModelIdentifier() {
        return DebuggerConstants.PLUGIN_ID;
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
        return running;
    }

    public void resume() throws DebugException {
        try{
            writeLock.lock();
            clearBreakpoints();
            setRunning(true);
            fireResumeEvent(DebugEvent.CLIENT_REQUEST);
            fJDIThread.resume();
        }finally{
            writeLock.unlock();
        }
    }
    
    @Override
    public void fireResumeEvent(int de){
        parent.resumed(this);
        super.fireResumeEvent(de);
    }

    public synchronized void suspend() throws DebugException {
		if (!isRunning())
			return;

		/** Suspends any pending step requests. */
		abortPendingStepRequests();

		if (isSuspending)
			return;

		/** Asynchronous thread suspension. */
		Runnable suspension = new Runnable() {
			public void run() {
				ThreadReference tr = getJDIThread();
				tr.suspend();
				int _timeout = TIMEOUT;
				long timeout = System.currentTimeMillis() + _timeout;
				boolean suspended = tr.isSuspended();

				/**
				 * If thread hasn't suspended, waits for a while until it does.
				 */
				while (!suspended && System.currentTimeMillis() < timeout) {
					try {
						synchronized (this) {
							wait(50);
						}
					} catch (InterruptedException ex) { }
					suspended = tr.isSuspended();
					if (suspended)
						break;
				}

				/** Timed out, thread can't be suspended. Issue an error. */
				if (!suspended) {
					logger.error("Failed to suspend thread.");
					return;
				}
				setRunning(false);
                synchronized(JavaThread.this){
                    parent.suspended(JavaThread.this);
                }
				fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
			}
		};

		Thread _suspension = new Thread(suspension);
		_suspension.start();
	}
    
    @Override
    public void fireSuspendEvent(int detail){
        parent.suspended(this);
        super.fireSuspendEvent(detail);
    }
    
   
    public void suspendedByVM(){
        this.setRunning(false);
        parent.suspended(this);
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
                fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
                this.requestFailed("Error while suspending thread.", e);                 
            }
        }
    }

    private void abortPendingStepRequests(){
    	if(pendingHandler != null) pendingHandler.abort();
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
            return fJDIThread.isSuspended() && (this.getTopStackFrame() != null);
        }catch(DebugException ex){
            logger.error("Error while querying thread's step capabilities.", ex);
            return false;
        }
    }

    public boolean isStepping() {
        return isStepping.get();
    }
    
    protected void setStepping(boolean mode){
        assert isStepping.compareAndSet(mode, !mode);
    }
    
    protected void setRunning(boolean mode){
    	this.running = mode;
        if(mode == true) clearBreakpoints();
    }
    

    public void stepInto() throws DebugException {
        StepHandler sh = new StepHandler(StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        sh.step();
    }

    public void stepOver() throws DebugException {
		StepHandler sh = new StepHandler(StepRequest.STEP_LINE,
				StepRequest.STEP_OVER);
		sh.step();
	}

    public void stepReturn() throws DebugException {
		StepHandler sh = new StepHandler(StepRequest.STEP_LINE,
				StepRequest.STEP_OUT);
		sh.step();
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
        this.parent = dt;
        return this.isSuspended();
    }

    public synchronized void unbindFromParentDT() {
        this.parent = null;
    }

    public void clearPendingStepRequests() {
        JDIMiscUtil.getInstance().clearPreviousStepRequests(this.getJDIThread(), getVMM());
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
        return this.parent;
    }

    private class StepHandler extends AbstractEventProcessor {
        
        private int granularity;
        private int depth;
        
        private EventRequest ourRequest;
        
        private volatile boolean done = false;
        
        public StepHandler(int granularity, int depth){
            this.depth = depth;
            this.granularity = granularity;
        }
        
        public synchronized void step() throws DebugException{
        	/** Places the step request. */
            placeStepRequest();			
            
            /** Register as listener to all JDI events 
             * which carry our request as event request.
             */
            registerAsListener();
            
            /** Resumes the underlying thread. */
            resumeUnderlyingThread();	
        }
        
        private void stepComplete() {
    		setRunning(false);
            clearStepRequest();
            finalizeHandler();
    	}
        
        private void finalizeHandler(){
            IJavaNodeManager vmm = getVMM();
            try{
                if(ourRequest != null)
                    vmm.getEventManager().removeEventListener(ourRequest, this);
            }catch(Throwable t){
                logger.error("Error while cleaning up step handler", t);
            }
            
            JavaThread.this.setStepping(false);
            fireSuspendEvent(DebugEvent.STEP_END);
        }
        
        private void clearStepRequest(){
            try{
                if(ourRequest != null){
                    getVMM().virtualMachine().eventRequestManager().
                        deleteEventRequest(ourRequest);
                }
            }catch(Throwable t){
                logger.error("Failed to remove old step requests. Further attempts to "
                                + "step this thread may result in error.");
            }
        }
        
        private void resumeUnderlyingThread() throws DebugException {
    		JavaThread.this.pendingHandler = this;
    		fireResumeEvent(mapStep(depth));
            JavaThread.this.setStepping(true);
    		JavaThread.this.resume();
    	}
    
        public synchronized void specializedProcess(Event e) {
            EventRequest incoming = e.request();
            assert ourRequest == incoming;
            stepComplete();
        }
        
        protected void placeStepRequest() throws DebugException{
            VirtualMachine underVM = null;
            try{
                VirtualMachineManager vmm = getVMM();
                underVM = vmm.virtualMachine();
                StepRequest sr =
                    underVM.eventRequestManager().createStepRequest(getJDIThread(), granularity, depth);
                sr.setSuspendPolicy(PolicyManager.getInstance().getPolicy(StepRequest.class));
                ourRequest = sr;
                ourRequest.enable();

                // This line is required to feed one of the many mistakes I made in the past 
                ourRequest.putProperty(DebuggerConstants.VMM_KEY, vmm.getName());
                
            }catch(Throwable t){
                finalizeHandler();
                GODBasePlugin.throwDebugExceptionWithError("Failed to set step request.", t);
            }
        }
        
        protected void registerAsListener(){
            IJavaNodeManager vmm = getVMM();
            vmm.getEventManager().addEventListener(ourRequest, this);
        }
        
        protected int mapStep(int stepCode){
            return stepMap.get(stepCode);
        }
        
        public synchronized void abort() {
            if(done) return;
            clearStepRequest();
    		finalizeHandler();
    	}
    }
}
