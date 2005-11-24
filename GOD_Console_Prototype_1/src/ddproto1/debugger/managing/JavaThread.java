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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

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

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.MessageHandler;

/**
 * 
 * Wrapper for a Java thread. JDT's poorer cousin. 
 * 
 * @author giuliano
 *
 */
public class JavaThread extends JavaDebugElement implements IThread{
	
	/** Move this to the debugger preferences. */
	private static final int TIMEOUT = 5000;
    
    private static Logger logger = MessageHandler.getInstance().getLogger(JavaThread.class);
    
    private static Map<Integer, Integer> stepMap = new HashMap<Integer, Integer>();
    static{
        stepMap.put(StepRequest.STEP_INTO, DebugEvent.STEP_INTO);
        stepMap.put(StepRequest.STEP_OVER, DebugEvent.STEP_OVER);
        stepMap.put(StepRequest.STEP_OUT, DebugEvent.STEP_RETURN);
    }
    
    /** JDI thread delegate. */
    private ThreadReference tDelegate; 
    
    private boolean isStepping = false;
    
    private boolean isSuspending = false;
    
    private boolean running = false;
    
    /** Lock that won't allow resume operations to be carried out at the same time
     * as operations that require thread suspension are being carried out. But will 
     * allow concurrent execution of the second type of operation.
     */ 
    private ReentrantReadWriteLock stackLock = new ReentrantReadWriteLock(false); 
    private Lock readLock = stackLock.readLock();
    private Lock writeLock = stackLock.writeLock();
    
    /** Pending step request handler (null if none) */
    StepHandler pendingHandler;
    
    /** List of breakpoints that last suspended this thread. */
    private List <IBreakpoint> sBreakpoints = new ArrayList<IBreakpoint>();
    
    public JavaThread(ThreadReference tDelegate, IJavaDebugTarget parent){
        super(parent);
        this.tDelegate = tDelegate;
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
            if(running) return null;
            locked = true;
            try{
                if(tDelegate.frameCount() == -1) return null; 
                List<StackFrame> frames = tDelegate.frames();
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
        return !running;
    }

    public int getPriority() throws DebugException {
        Field pField = null;
        try {
            pField = tDelegate.referenceType().fieldByName("priority");
            if (pField == null) {
                requestFailed("Could not access thread priority field. ", null); 
            }
            Value priority = tDelegate.getValue(pField);
            
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
            if(running) return null;
            if(tDelegate.frameCount() == -1) return null;
            return new JavaStackframe(this, tDelegate.frame(0));
        }catch(IncompatibleThreadStateException ex){
            requestFailed("Error while acquiring top stack frame.", ex);
            return null;
        }finally{
            if(locked) readLock.unlock();
        }
    }

    public String getName() throws DebugException {
        return tDelegate.name();
    }

    public IBreakpoint[] getBreakpoints() {
    	return breakpointsAsArray();
    }
    
    public void handleBreakpointHit(JavaBreakpoint jb){
        addBreakpoint(jb);
        fireSuspendEvent(DebugEvent.BREAKPOINT);
    }
    
    protected IBreakpoint [] breakpointsAsArray(){
        synchronized(sBreakpoints){
            return sBreakpoints.toArray(new IBreakpoint[sBreakpoints.size()]);
        }
    }
    
    protected void addBreakpoint(JavaBreakpoint jb){
        synchronized(sBreakpoints){
            sBreakpoints.add(jb);
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
        return !running;
    }

    public boolean canSuspend() {
        return running;
    }

    public boolean isSuspended() {
        return !running;
    }

    public void resume() throws DebugException {
        try{
            writeLock.lock();
            clearBreakpoints();
            setRunning(true);
            fireResumeEvent(DebugEvent.CLIENT_REQUEST);
            tDelegate.resume();
        }finally{
            writeLock.unlock();
        }
    }

    public synchronized void suspend() throws DebugException {
    	if(!running) return;
    	
    	/** Suspends any pending step requests. */
    	abortPendingStepRequests();
    	
    	if(isSuspending) return;
    	
    	/** Asynchronous thread suspension.     */
    	Runnable suspension = new Runnable(){
    		public void run(){
    			ThreadReference tr = getJDIThread();
    			tr.suspend();
    			int _timeout = TIMEOUT;
    			long timeout = System.currentTimeMillis() + _timeout;
    			boolean suspended = tr.isSuspended();
    			
    			/** If thread hasn't suspended, waits for a while until
    			 * it does.
    			 */
    			while(!suspended && System.currentTimeMillis() < timeout){
    				try{
    					synchronized(this) { wait(50); }
    				}catch(InterruptedException ex) { }
    				suspended = tr.isSuspended();
    				if(suspended) break;
    			}
    			
    			/** Timed out, thread can't be suspended. Issue an error. */
    			if(!suspended){
    				logger.error("Failed to suspend thread.");
    				return;
    			}
    			setRunning(false);
                fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
    		}	
    	};
    	
    	Thread _suspension = new Thread(suspension);
    	_suspension.start();
    }
    
    public void suspendedByVM(){
        this.setRunning(false);
    }
    
    public void resumedByVM()
        throws DebugException
    {
        setRunning(true);
        ThreadReference thread = this.getJDIThread();
        
        /** We do the same semantic twisting of VirtualMachine#resume as 
         * the one we found inside JDT. Seems like the best way to avoid
         * bizarre suspend count configurations.
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
            return tDelegate.isSuspended() && (this.getTopStackFrame() != null);
        }catch(DebugException ex){
            logger.error("Error while querying thread's step capabilities.", ex);
            return false;
        }
    }

    public boolean isStepping() {
        return isStepping;
    }
    
    protected void setStepping(boolean mode){
        isStepping = mode;
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
    	StepHandler sh = new StepHandler(StepRequest.STEP_LINE, StepRequest.STEP_OVER);
    	sh.step();
    }

    public void stepReturn() throws DebugException {
    	StepHandler sh = new StepHandler(StepRequest.STEP_LINE, StepRequest.STEP_OUT);
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
        return tDelegate;
    }
    
    private class StepHandler extends AbstractEventProcessor {
        
        private int granularity;
        private int depth;
        
        private EventRequest ourRequest;
        
        public StepHandler(int granularity, int depth){
            this.depth = depth;
            this.granularity = granularity;
        }
        
        public void step() throws DebugException{
        	/** Places the step request. */
            placeStepRequest();			
            /** Register as listener to all JDI events 
             * which carry our request as event request.
             */
            registerAsListener();
            /** Resumes the underlying thread. */
            resumeUnderlyingThread();	
        }
        
        public void stepComplete(){
        	setRunning(false);
        	finalizeHandler();
        }
        
        private void finalizeHandler(){
            VirtualMachineManager vmm = getVMM();
            if(ourRequest != null)
            	vmm.getEventManager().removeEventListener(ourRequest, this);
            JavaThread.this.pendingHandler = null;
        }
        
        public void resumeUnderlyingThread() throws DebugException{
        	JavaThread.this.pendingHandler = this;
            fireResumeEvent(mapStep(depth));
        	JavaThread.this.resume();
        }

        public void specializedProcess(Event e) {
            EventRequest incoming = e.request();
            assert ourRequest == incoming;
            JavaThread.this.setStepping(false);
            fireSuspendEvent(DebugEvent.STEP_END);
            stepComplete();
        }
        
        protected void placeStepRequest() throws DebugException{
            VirtualMachineManager vmm = getVMM();
            VirtualMachine underVM = vmm.virtualMachine();
            StepRequest sr =
                underVM.eventRequestManager().createStepRequest(getJDIThread(), granularity, depth);
            try{
                vmm.getEventManager().addEventListener(IEventManager.STEP_EVENT, this);
            }catch(IllegalAttributeException ex){
                requestFailed("Failed to place step request.", ex);
            }
        
            ourRequest = sr;
        }
        
        protected VirtualMachineManager getVMM(){
            return JavaThread.this.getJavaDebugTarget().getVMManager();
        }
        
        protected void registerAsListener(){
            VirtualMachineManager vmm = getVMM();
            vmm.getEventManager().addEventListener(ourRequest, this);
        }
        
        protected int mapStep(int stepCode){
            return stepMap.get(stepCode);
        }
        
        public void abort(){
        	if(ourRequest != null){
        		VirtualMachine vm = getVMM().virtualMachine();
        		vm.eventRequestManager().deleteEventRequest(ourRequest);
            	finalizeHandler();
        	}
        }
    }
}
