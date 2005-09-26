/*
 * Created on Sep 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ddproto1.debugger.managing.tracker;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.BreakpointRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.exception.IllegalStateException;
import ddproto1.exception.InternalError;
import ddproto1.exception.PropertyViolation;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.interfaces.ISemaphore;
import ddproto1.util.Semaphore;

/**
 * This class represents the actual Distributed Thread.
 *
 * 15/02 - Lots of complicated locking mechanisms that I can't actually really
 *         say are critical. 
 */
public class DistributedThread {
    
    private static final VMManagerFactory vmmf = VMManagerFactory.getInstance();

    /* Possible thread states. */
    public static final byte UNKNOWN = -1;
    public static final byte STEPPING_REMOTE = DebuggerConstants.STEPPING_REMOTE;
    public static final byte STEPPING_INTO = DebuggerConstants.STEPPING_INTO;
    public static final byte STEPPING_OVER = DebuggerConstants.STEPPING_OVER;
    public static final byte RUNNING = DebuggerConstants.RUNNING;
    public static final byte SUSPENDED = DebuggerConstants.SUSPENDED;
    
    /* Global Universally Unique ID for this Distributed thread. */
    private int uuid;
    
    /* FIXME Have no idea what's those attributes are for. */
    private int stackLevel = UNKNOWN;
    private BreakpointRequest nextBp;
    
    /* Virtual stack for this thread. */
    private VirtualStack vs;
    
    /* Debugger thread currently manipulating this "mirror" */
    private Thread owner;
    
    private boolean damaged = false;
    private int damaged_frame = UNKNOWN;
    
    /* Semaphore that avoids that a frame gets popped at the same time
     * a thread is suspended. I think it works because the debuggee notification
     * is synchronous and won't return if the pop doesn't:
     * 
     * Debuggee                    Debugger
     *     O -----------------------> 0
     *           SIGNAL_BOUNDARY      | (pop frame)
     *     0 <------------------------0
     *     |           OK
     *     |
     *    ...
     * 
     * Since the OK is never issued if the popping is blocked, the client issuing
     * SIGNAL_BOUNDARY will also be blocked, hence things will end up working out
     * just fine (albeit "a bit" slow).
     *     
     * protected ISemaphore popSuspendLock = new Semaphore(1); (COMMENTED SEMAPHORE)
     *
     * This semaphore is for avoiding that the thread gets resumed after a stack 
     * inspection operation has begun. (OLD COMMENT)
     * 
     * I realized the lock is just for avoiding modifications to the stack in two
     * distinct situations:
     * 
     * 1) The thread is suspended. If the thread is suspended, no frames should be
     *    allowed to be popped off from the stack.
     * 2) The stack is being inspected. During inspection, the stack should not be
     *    modified. 
     *    
     * I could add another lock to avoid thread resumption while a inspection operation
     * takes place, but if I lock the stack from being modified that's already enough.
     * Therefore, all I need is a read-write lock. 
     * 
     * 
     * ReentrantReadWriteLock is buggy when set to fair mode. Therefore we'll use it in 
     * unfair mode.
     */
    protected ReadWriteLock stackInspectionLock = new ReentrantReadWriteLock(false);  
    private final Lock rsLock = stackInspectionLock.readLock();
    private final Lock wsLock = stackInspectionLock.writeLock();
    
    private IHeadCounter counter;

    /* Actual thread state. */
    private byte state;
    
    public DistributedThread(VirtualStackframe root, IVMThreadManager tm, IHeadCounter counter){
        this(root, tm, UNKNOWN, counter);
    }
    
    public DistributedThread(VirtualStackframe root, IVMThreadManager tm, byte initialState, IHeadCounter counter){
        this.uuid = root.getLocalThreadId().intValue();
        vs = new VirtualStack();
        vs.pushFrame(root);
        state = initialState;
        this.counter = counter;
    }
    
    /**
     * Returns the VirtualStackframe on top of the virtual callstack.
     * 
     * REMARK Redundant, since we can get it from the stack directly.
     * 
     * @return
     */
    public VirtualStackframe getHead(){
        return vs.peek();
    }
    
    /**
     * Returns the Global Universally Unique ID for this distributed
     * thread. The GUUID structure is as follows:
     * 
     * [8  bit] -----> Node for the root thread (most significant bits).
     * [24 bit] -----> Locally assigned ID for the root thread.
     *    
     * @return integer GUUID
     */
    public int getId(){
        return uuid;
    }
    
    /**
     * Returns a reference to this thread's virtual stack.
     * 
     * @return The virtual stack.
     */
    public VirtualStack virtualStack(){
        return vs;
    }
    
    public boolean isDamaged(){
        return damaged;
    }
    
    public int firstDamaged(){
        return damaged_frame;
    }
    
    /**
     * Returns the intended mode for the thread. You should not trust that the value returned by this method
     * actually reflects the true state of the thread, since the function of the 'mode' variable is to convey
     * intention and not real state.
     * 
     * @return
     */
    protected byte getMode(){
        return state; 
    }
   
    public boolean isLocked(){
        return owner != null;
    }
    
    public boolean isCurrentOwner(){
        return owner == Thread.currentThread();
    }
    
    /**
     * This checks if the distributed thread is "really" suspended, that is, if it's head is actually suspended.
     * 
     * @return
     */
    public boolean isSuspended(){
        ThreadReference head = this.getLockedHead();
        boolean result = head.isSuspended();
        rsLock.unlock();
        return result;
    }
    
    /**
     * Like isSuspended, checks if the head thread is actually in step mode. 
     * 
     * @return
     */
    public boolean isStepping(){
        ThreadReference head = this.getLockedHead();
        boolean result = head.isSuspended();
        rsLock.unlock();
        return result;
    }
    
    /**
     * Private method, locks the stack and peeks the head. It's up to the sender to unlock
     * the stack.
     */
    private ThreadReference getLockedHead(){
        checkOwner();
        rsLock.lock();
        Byte nodeGID = vs.unlockedPeek().getLocalThreadNodeGID();
        Integer lt_uuid = vs.unlockedPeek().getLocalThreadId();
        ThreadReference head = vmmf.getVMManager(nodeGID).getThreadManager().findThreadByUUID(lt_uuid);
        if(head == null) throw new PropertyViolation("Error! Thread's head no longer exists. Please report this bug.");
        
        return head;
    }
         
    /**
     * Locks the current thread. No other thread will be able to
     * modify it without getting a <b>ddproto1.exception.IllegalStateException</b>.
     * This is for when someone needs to conduct a series of operations to the thread
     * and ensure that no-one messes it up in the meantime. 
     * 
     * @throws IllegalStateException if a thread tries to modify it without locking first.
     */
    public synchronized void lock() {
        try{
            while(owner != null){
                this.wait();
            }
            
            owner = Thread.currentThread();
            
        }catch(InterruptedException e){ }
    }
    
    /** 
     * Releases the modification lock.
     * 
     * @throws IllegalStateException if the thread is not the owner of the lock.
     */
    public synchronized void unlock() {
        checkOwner();
        owner = null;
        this.notify();
    }

	/* Control methods */
	public void resume(){
	    checkOwner();  // Only one thread per time.
        try {
            if (!(((state & STEPPING_INTO) != 0) || ((state & SUSPENDED) != 0)))
                throw new IllegalStateException(
                        "You cannot resume a thread that hasn't been stopped.");

            /*
             * This method doesn't have to be synchronized since the hipothesis
             * is that the head thread is already stopped (and hence there's no
             * risk of it being popped). TODO Test what happens if we kill the
             * remote JVM.
             */
            VirtualStackframe head = getHead();
            IVMThreadManager tm = vmmf.getVMManager(
                    head.getLocalThreadNodeGID()).getThreadManager();

            ThreadReference tr = tm.findThreadByUUID(head.getLocalThreadId());
            if (tr == null)
                throw new IllegalStateException("Head thread no longer exists.");

            state = RUNNING;
            tr.resume();
        } finally {
            rsLock.unlock();
        }
	}
    
    public void suspend(){
        checkOwner();
        
	    if(!(state == RUNNING))
	        throw new IllegalStateException(
	                "You cannot suspend a thread that is not running.");
	    /* Downs the semaphore to avoid that our thread gets popped after
	     * it has been suspended. Any threads that signalled popping will 
	     * be blocked. 
	     */
	    rsLock.lock();
	    VirtualStackframe head = getHead();
	    IVMThreadManager tm = vmmf.getVMManager(head.getLocalThreadNodeGID())
                .getThreadManager();
	    ThreadReference tr = tm.findThreadByUUID(head.getLocalThreadId());
	    if(tr == null)
	        throw new InternalError("Error while suspending: head thread no longer exists.");
	    tr.suspend();
	    state = SUSPENDED;
	}

    protected void setDamaged(boolean damage){
        this.damaged = damage;
        this.damaged_frame = UNKNOWN;
    }

    
    protected void setDamaged(boolean damage, int start_frame){
        this.damaged = damage;
        this.damaged_frame = start_frame;
    }
    
    protected void unsetStepping(){
        checkOwner();
        state = RUNNING;
    }
    
	protected void setStepping(byte stepMode)
        throws InvalidAttributeValueException
    {
	    checkOwner();
        state = stepMode;
	}

	/* ThreadLocal metaphors */
	protected void setNextBreakpoint(BreakpointRequest bp){
	    checkOwner();
	    if(this.nextBp != null)
	        throw new IllegalStateException(
	                "The distributed thread tracker somehow "
	                        + " saw the distributed "
	                        + this
	                        + " thread making two concurrent requests. This is a "
	                        + "serious inconsistency.");
	    this.nextBp = bp;
	}
    
    protected BreakpointRequest getNextBreakpoint(){
        return nextBp;
    }

	protected int getReturnAddress(){
	    return stackLevel;
	}

	protected void setReturnAddress(int stackLevel){
	    checkOwner();
        if(this.stackLevel != UNKNOWN)
            throw new IllegalStateException(
                    "The distributed thread tracker somehow "
                            + " saw the distributed "
                            + this
                            + " thread making two concurrent requests. This is a "
                            + "serious inconsistency.");

        this.stackLevel = stackLevel;
    }
	
	protected void checkOwner()
		throws IllegalStateException
	{
	    if(owner == null) return;
	    if(!Thread.currentThread().equals(owner))
	        throw new IllegalStateException("Current thread not owner.");
	}

	/**
	 * Contrary to the Java SDK implementation, we <b>will</b> have a metaobject
	 * representing the call stack. That is exactly what this object represents - 
	 * te virtual call stack for a distributed thread.
	 * 
	 * One might ask why. Well...
	 * 
	 * <ol>
	 * <li> Take a look at the polluted ThreadReference interface. </li>
	 * <li> Try to introspect the call stack in a java program. </li>
	 * </ol>
	 * 
	 * The <b>ThreadReference</b> class concetrates all methods for accessing
	 * the call stack, saturating its interface and decreasing cohesion. However,
	 * since the call stack is thread-related, that is not really a big problem.
	 * The worse part is if you try to introspect the call stack from a regular java
	 * program (not through the Java Debug Interface). After spending a few hours 
	 * on the problem, you might find out that you need the following code:
	 * 
	 * <code>
	 *	Throwable ex = new Throwable();
	 *	StackTraceElement [] stackTrace = ex.getStackTrace();
	 * </code>
	 * 
	 * That's quite honestly very ugly. Not to mention that the Throwable class is
	 * probably the last one you'll look for that information. Not to mention that 
	 * you probably wouldn't know that by simply creating a Throwable it acquires 
	 * knowledge about the call stack that you cannot find anywhere else in the Java 
	 * API.
	 * 
	 * 15/02/2005 - Decided to move this class into the DistributedThread class since they
	 * shared so much state anyway. The coupling was getting promiscuous.
	 *  
	 * @author giuliano
	 *
	 */
	public class VirtualStack {
	    private Stack<VirtualStackframe> frameStack = new Stack<VirtualStackframe>();
	    
	    public void pushFrame(VirtualStackframe tr) {
            checkOwner();
            try {
                wsLock.lock();
                frameStack.push(tr);
            } finally {
                wsLock.unlock();
            }
        }

        public DistributedThread parentDT() {
            return DistributedThread.this;
        }

        public VirtualStackframe popFrame() {
            checkOwner();
            try {
                /*
                 * Popping only applies to threads that have not been suspended.
                 * If the thread gets suspended after the pop signal has reached
                 * the server but before the handler had a chance to actually
                 * pop the thread, then synchronization is required.
                 * 
                 * What this semaphore does is block the handler thread from
                 * popping the application thread until it gets resumed by the
                 * user who suspended it.
                 */
                wsLock.lock();
                VirtualStackframe popped = (VirtualStackframe) frameStack.pop();
                return popped;
            } finally {
                wsLock.unlock();
            }
        }
	    
	    public VirtualStackframe peek() throws IllegalStateException{
            try{
                rsLock.lock();
                return this.unlockedPeek();
            }finally{
                rsLock.unlock();
            }
	    }
        
        protected VirtualStackframe unlockedPeek(){
            return(VirtualStackframe) frameStack.peek();
        }
	    
	    public int getVirtualFrameCount() throws IllegalStateException{
            try{
                rsLock.lock();
                return frameStack.size();
            }finally{
                rsLock.unlock();
            }
	    }
	    
	    public VirtualStackframe getVirtualFrame(int idx)
	    	throws NoSuchElementException, IllegalStateException
	    {
            try{
                rsLock.lock();
                if (frameStack.size() <= idx)
                    throw new NoSuchElementException("Invalid index - " + idx);

                return (VirtualStackframe) frameStack.elementAt(frameStack
                        .size()
                        - idx - 1);
            } finally {
                rsLock.unlock();
            }
	    }
	    
	    public List <VirtualStackframe>virtualFrames(int start, int length) 
            throws NoSuchElementException, IllegalStateException
        {
             try{
                rsLock.lock();
                return frameStack.subList(start, start + length - 1);
            }finally{
                rsLock.unlock();
            }
	    }
        
        public int getDTRealFrameCount(){
            try{
                rsLock.lock();
                
                int frameCount = 0;
                int initial = frameStack.size()-1;
                for(int k = initial; k >= 0; k--){
                    VirtualStackframe cFrame = frameStack.get(k);
                    int topFrame, baseFrame;
                    
                    if(k == initial)
                        topFrame = counter.headFrameCount(cFrame.getLocalThreadId());
                    else
                        topFrame = cFrame.getCallTop();
                    
                    if(k == 0)
                        baseFrame = 1;
                    else
                        baseFrame = cFrame.getCallBase();
                                            
                    frameCount += topFrame - baseFrame + 1;
                }
                return frameCount;
            }finally{
                rsLock.unlock();
            }
        }
        
        public int length(){
            return frameStack.size();
        }
        
        public IRealFrame mapToRealFrame(int DTFrame)
            throws NoSuchElementException
        {
            try{
                rsLock.lock();
                int initial = frameStack.size()-1;
                for(int k = initial; k >= 0; k--){
                    VirtualStackframe cFrame = frameStack.get(k);
                    int topFrame, baseFrame;
                    
                    if(k == initial)
                        topFrame = counter.headFrameCount(cFrame.getLocalThreadId());
                    else
                        topFrame = cFrame.getCallTop();
                    
                    if(k == 0)
                        baseFrame = 1;
                    else
                        baseFrame = cFrame.getCallBase();
                    
                    int frames = topFrame - baseFrame + 1;
                    if(DTFrame - frames < 0){
                        final int theFrame = DTFrame + counter.headFrameCount(cFrame.getLocalThreadId(), true) - topFrame;
                        final int ltuuid = cFrame.getLocalThreadId();
                        return new IRealFrame(){
                            public int getLocalThreadUUID() { return ltuuid; }
                            public int getFrame() { return theFrame; }
                        };
                    }
                    DTFrame -= frames;
                }
                
            }finally{
               rsLock.unlock();
            }
            
            throw new NoSuchElementException("Invalid frame " + DTFrame + ".");
        }
	}
}
