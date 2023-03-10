/*
 * Created on Sep 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ddproto1.debugger.managing.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.INodeManager;
import ddproto1.debugger.managing.IThreadManager;
import ddproto1.exception.IllegalStateException;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * This class represents the actual Distributed Thread.
 * 
 * It currently contains lots of complicated locking mechanisms that I can't
 * actually really say are critical.
 * 
 * To add insult to injury, this class isn't even thread-safe. I began fixing
 * it, but it'll probably be a while before I can get it right.
 */
public class DistributedThread extends GODDebugElement implements IThread,
        IDistributedThread {

    private static final ConversionUtil cUtil = ConversionUtil.getInstance();

    private static final Logger logger = MessageHandler.getInstance()
            .getLogger(DistributedThread.class);

    /* Possible thread states. */
    public static final byte UNKNOWN = DebuggerConstants.MODE_UNKNOWN;

    public static final byte ILLUSION = DebuggerConstants.ILLUSION;

    public static final byte STEPPING = DebuggerConstants.STEPPING;

    public static final byte RUNNING = DebuggerConstants.RUNNING;

    public static final byte SUSPENDED = DebuggerConstants.SUSPENDED;

    public static final byte TERMINATED = DebuggerConstants.TERMINATED;

    private static final int VIRTUALSTACK_TOPIDX = 1;

    private static final int TOP_INDEX = 0;

    /* Global Universally Unique ID for this Distributed thread. */
    private int uuid;

    private void setStepping(byte stepMode) {
        ConversionUtil ct = ConversionUtil.getInstance();
        MessageHandler.getInstance().getDebugOutput().println(
                "DT " + ct.uuid2Dotted(this.uuid) + " state set to "
                        + ct.statusText(stepMode));

        setMode(stepMode);
    }

    /** Set of suspended elements. */
    private final Set<IThread> suspended = new HashSet<IThread>();

    /** Set of component elements. */
    private final Set<IThread> actualElements = new HashSet<IThread>();

    private final AtomicReference<RemoteStepHandler> pendingHandler = new AtomicReference<RemoteStepHandler>();

    /** List of breakpoints currently affecting this DT. */
    private final List<IBreakpoint> breakpoints = Collections
            .synchronizedList(new LinkedList<IBreakpoint>());

    /* Virtual stack for this thread. */
    private final VirtualStack vs = new VirtualStack();

    private final DistributedThreadManager fDTM;

    /* Debugger thread currently manipulating this "mirror" */
    private Thread owner;

    private boolean damaged = false;

    private int damaged_frame = UNKNOWN;

    /*
     * Semaphore that avoids that a frame gets popped at the same time a thread
     * is suspended. I think it works because the debuggee notification is
     * synchronous and won't return if the pop doesn't:
     * 
     * Debuggee Debugger O -----------------------> 0 SIGNAL_BOUNDARY | (pop
     * frame) 0 <------------------------0 | OK | ...
     * 
     * Since the OK is never issued if the popping is blocked, the client
     * issuing SIGNAL_BOUNDARY will also be blocked, hence things will end up
     * working out just fine (albeit "a bit" slow).
     * 
     * protected ISemaphore popSuspendLock = new Semaphore(1); (COMMENTED
     * SEMAPHORE)
     * 
     * This semaphore is for avoiding that the thread gets resumed after a stack
     * inspection operation has begun. (OLD COMMENT)
     * 
     * I realized the lock is just for avoiding modifications to the stack in
     * two distinct situations:
     * 
     * 1) The thread is suspended. If the thread is suspended, no frames should
     * be allowed to be popped off from the stack. 2) The stack is being
     * inspected. During inspection, the stack should not be modified.
     * 
     * I could add another lock to avoid thread resumption while a inspection
     * operation takes place, but if I lock the stack from being modified that's
     * already enough. Therefore, all I need is a read-write lock.
     * 
     * 
     * ReentrantReadWriteLock is buggy when set to fair mode. Therefore we'll
     * use it in unfair mode.
     */
    protected final ReadWriteLock stackInspectionLock = new ReentrantReadWriteLock(
            false);

    private final Lock rsLock = stackInspectionLock.readLock();

    private final Lock wsLock = stackInspectionLock.writeLock();

    /* Actual thread state. */
    private volatile byte state;

    private String name;

    public DistributedThread(VirtualStackframe root,
            DistributedThreadManager dtm, IDebugTarget parentManager) {
        super(parentManager);
        uuid = root.getLocalThreadId().intValue();
        vs.pushFrameInternal(root);
        setMode(RUNNING);
        name = cUtil.uuid2Dotted(this.getId());
        fDTM = dtm;
    }

    /**
     * Returns the VirtualStackframe on top of the virtual callstack.
     * 
     * REMARK Redundant, since we can get it from the stack directly.
     * 
     * @return
     */
    public VirtualStackframe getHead() {
        return vs.peek();
    }

    /**
     * Returns the Global Universally Unique ID for this distributed thread. The
     * GUUID structure is as follows:
     * 
     * [8 bit] -----> Node for the root thread (most significant bits). [24 bit]
     * -----> Locally assigned ID for the root thread.
     * 
     * @return integer GUUID
     */
    public int getId() {
        return uuid;
    }

    /**
     * Returns a reference to this thread's virtual stack.
     * 
     * @return The virtual stack.
     */
    public VirtualStack virtualStack() {
        return vs;
    }

    /**
     * Returns the damage status of this distributed thread. Damaged distributed
     * threads may contain inconsistencies in their internal representation, and
     * therefore can't be trusted as reliable debugging entitites.
     * 
     * @return
     */
    public boolean isDamaged() {
        return damaged;
    }

    /**
     * @deprecated
     * @return
     */
    public int firstDamaged() {
        return damaged_frame;
    }

    /**
     * Returns the intended mode for the thread. You should not trust that the
     * value returned by this method actually reflects the true state of the
     * thread, since the function of the 'mode' variable is to convey intention
     * and not real state.
     * 
     * @return
     */
    protected byte getMode() {
        return state;
    }

    protected void setMode(byte mode) {
        this.state = mode;
    }

    public boolean isLocked() {
        return owner != null;
    }

    public boolean isCurrentOwner() {
        return owner == Thread.currentThread();
    }

    /**
     * A distributed thread is suspended when all of its component threads are
     * suspended.
     * 
     * @return
     */
    public boolean isSuspended() {
        return getMode() == SUSPENDED;
    }

    /**
     * Like isSuspended, checks if the head thread is actually in step mode.
     * 
     * @return
     */
    public boolean isStepping() {
        try {
            IThread head = this.getLockedHead();
            boolean result = head.isStepping();
            return result;
        } finally {
            rsLock.unlock();
        }
    }

    /**
     * Private method, locks the stack and peeks the head. It's up to the sender
     * to unlock the stack.
     */
    private ILocalThread getLockedHead() {
        // checkOwner();
        rsLock.lock();
        return vs.unlockedPeek().getThreadReference();
    }

    /**
     * Locks the current thread. No other thread will be able to modify it
     * without getting a <b>ddproto1.exception.IllegalStateException</b>. This
     * is for when someone needs to conduct a series of operations to the thread
     * and ensure that no-one messes it up in the meantime.
     * 
     * @throws IllegalStateException
     *             if a thread tries to modify it without locking first.
     */
    public synchronized void lock() {
        try {
            Thread current = Thread.currentThread();
            if (current.equals(owner))
                return; // reentrant lock.

            while (owner != null) {
                this.wait();
            }

            owner = Thread.currentThread();

        } catch (InterruptedException e) {
        }
    }

    /**
     * Releases the modification lock.
     * 
     * @throws IllegalStateException
     *             if the thread is not the owner of the lock.
     */
    public synchronized void unlock() {
        checkOwner();
        owner = null;
        this.notify();
    }

    /* Control methods */
    public void resume() throws DebugException {
        // checkOwner(); IThread operations can't require external locking to
        // succeed.
        try {
            /** Read lock to avoid popping while resumption in progress. */
            rsLock.lock();
            // if (!(((state & STEPPING_INTO) != 0) || ((state & SUSPENDED) !=
            // 0)))
            if (!isSuspended())
                throw new IllegalStateException(
                        "You cannot resume a thread that hasn't been stopped.");

            clearBreakpoints();

            /*
             * This method doesn't have to be synchronized since the hipothesis
             * is that the head thread is already stopped (and hence there's no
             * risk of it being popped). TODO Test what happens if we kill the
             * remote JVM.
             */
            VirtualStackframe head = getHead();
            IThread tr = head.getThreadReference();

            // setMode(RUNNING); - This is wrong. There may be other suspended
            // threads.
            tr.resume();
        } finally {
            rsLock.unlock();
        }
    }

    /**
     * Suspends the current distributed thread. Suspending a distributed thread
     * means suspending all of its participating local threads, including its
     * head.
     */
    public void suspend() throws DebugException {
        // checkOwner(); IThread operations can't require external locking to
        // succeed.
        try {
            IThread head = getLockedHead();
            head.suspend();

            /**
             * We must clear all pending step requests for this distributed
             * thread, because there might be a pending step over somewhere
             * along the stack.
             */
            for (Object _threadRef : vs.frameStack.asList()) {
                ILocalThread tr = (ILocalThread) _threadRef;
                try {
                    /**
                     * Quick note regarding damaged stackframes: these are
                     * either dead threads or threads that moved but shouldn't
                     * have moved (and we detected that because they reported
                     * some event). If they're dead, canSuspend will return
                     * false. If they moved and shouldn't have, they're likely
                     * to be already suspended.
                     */
                    if (tr.canSuspend())
                        tr.suspend();
                    tr.clearPendingStepRequests();
                } catch (Exception ex) {
                    logger.error("Error while suspending distributed thread.",
                            ex);
                }
            }

            setMode(SUSPENDED);
        } catch (Throwable t) {
            GODBasePlugin.throwDebugExceptionWithError(
                    "Error while suspending distributed thread.", t);
        } finally {
            rsLock.unlock();
        }
    }

    /**
     * Marks this Distributed Thread as being damaged. Damage happens as a
     * result of the detection of an irrecoverable inconsistency in the state of
     * the Distributed Thread.
     * 
     * @param damage
     */
    protected synchronized void flagAsDamaged() {
        this.damaged = true;
        this.damaged_frame = UNKNOWN;
    }

    protected void unsetStepping() {
        checkOwner();
        synchronized (suspended) {
            setMode(RUNNING);
        }
    }

    protected synchronized void checkOwner() throws IllegalStateException {
        // if(owner == null) return; - Why?!?!
        if (!Thread.currentThread().equals(owner))
            throw new IllegalStateException("Current thread not owner.");
    }

    /**
     * 
     * 15/02/2005 - Decided to move this class into the DistributedThread class
     * since they shared so much state anyway. The coupling was getting
     * promiscuous.
     * 
     * @author giuliano
     * 
     */
    public class VirtualStack {

        private final LinkedMap frameStack = new LinkedMap();

        public void pushFrame(VirtualStackframe tr) {
            checkOwner();
            try {
                wsLock.lock();
                this.pushFrameInternal(tr);
            } finally {
                wsLock.unlock();
            }
        }

        private void pushFrameInternal(VirtualStackframe vsf) {
            frameStack.put(vsf.getThreadReference(), vsf);
            ILocalThread cThread = vsf.getThreadReference();
            /**
             * Binds the local thread to the current distributed thread. We
             * synchronize because the CAS should be atomic.
             */
            synchronized (suspended) {
                vsf.setParent(DistributedThread.this);
                addHeadToTables(cThread, cThread
                        .setParentDT(DistributedThread.this));
            }
        }

        public IDistributedThread parentDT() {
            return DistributedThread.this;
        }

        public VirtualStackframe frameOfThread(IThread tr) {
            return (VirtualStackframe) frameStack.get(tr);
        }

        public VirtualStackframe popFrame() throws DebugException {
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
                VirtualStackframe popped = (VirtualStackframe) frameStack
                        .remove(frameStack.lastKey());
                ILocalThread tr = popped.getThreadReference();
                removeHeadFromTables(tr);
                return popped;
            } finally {
                wsLock.unlock();
            }
        }

        public VirtualStackframe peek() throws IllegalStateException {
            try {
                rsLock.lock();
                return this.unlockedPeek();
            } finally {
                rsLock.unlock();
            }
        }

        protected VirtualStackframe unlockedPeek() {
            return (VirtualStackframe) frameStack.get(frameStack.lastKey());
        }

        public int getVirtualFrameCount() throws IllegalStateException {
            try {
                rsLock.lock();
                return frameStack.size();
            } finally {
                rsLock.unlock();
            }
        }

        /**
         * This method is zero-based.
         * 
         * @param idx
         * @return
         * @throws NoSuchElementException
         * @throws IllegalStateException
         */
        public VirtualStackframe getVirtualFrame(int idx)
                throws NoSuchElementException, IllegalStateException {
            try {
                rsLock.lock();
                if (frameStack.size() <= idx)
                    throw new NoSuchElementException("Invalid index - " + idx);

                return (VirtualStackframe) frameStack.getValue(frameStack
                        .size()
                        - idx - 1);
            } finally {
                rsLock.unlock();
            }
        }

        public List<VirtualStackframe> virtualFrames(int start, int length)
                throws NoSuchElementException, IllegalStateException {
            try {
                rsLock.lock();
                return frameStack.asList().subList(start, start + length - 1);
            } finally {
                rsLock.unlock();
            }
        }

        public int length() {
            try {
                rsLock.lock();
                return frameStack.size();
            } finally {
                rsLock.unlock();
            }
        }
    }

    public IStackFrame[] getStackFrames() throws DebugException {
        try {
            rsLock.lock();
            if(!hasStackFrames()) return INodeManager.NO_CALLSTACK;
            VirtualStack vs = this.virtualStack();
            int fCount = vs.getVirtualFrameCount();

            ArrayList<IStackFrame> allFrames = new ArrayList<IStackFrame>(
                    fCount);

            if (logger.isDebugEnabled()) {
                logger.debug("Getting stackframes for distributed thread "
                        + cUtil.uuid2Dotted(getId()) + ".");
            }

            /** For each virtual stack frame... */
            for (int i = 1; i <= fCount; i++) {
                VirtualStackframe vsf = vs.getVirtualFrame(i - 1); // getVirtualFrame
                                                                    // is
                                                                    // zero-based.
                IThread cThread = vsf.getThreadReference();

                /**
                 * If the thread isn't stopped or it cannot be referenced, adds
                 * a "thread frame" to the stack.
                 */
                IStackFrame[] realFrames;
                realFrames = cThread.getStackFrames();

                // length == 0 means no frames or non-stopped thread.
                if (realFrames.length == 0) {
                    allFrames.add(vsf);
                    continue;
                }

                /**
                 * Extracts the indexes of the real top and base frames
                 * according to the current length of the call stack.
                 */
                int callBase = realBaseFrame(vsf, i, realFrames.length);
                int callTop = realTopFrame(vsf, i, realFrames.length);

                /* Pushes frames from base to top */
                for (int k = callTop; k <= callBase; k++)
                    allFrames.add(new LocalStackframeWrapper(this,
                            realFrames[k], getDebugTarget()));
            }

            IStackFrame[] isf = new IStackFrame[allFrames.size()];

            if (logger.isDebugEnabled()) {
                logger.debug("Got stackframes (" + isf.length
                        + ") from distributed thread "
                        + cUtil.uuid2Dotted(getId()) + ".");
            }

            return allFrames.toArray(isf);

        } finally {
            rsLock.unlock();
        }
    }

    public boolean hasStackFrames() throws DebugException {
        return isSuspended();
    }

    public int getPriority() throws DebugException {
        return UNKNOWN;
    }

    public IStackFrame getTopStackFrame() throws DebugException {
        // Straightforward, simple and correct, but inefficient 
        // implementation.
        IStackFrame [] frames = getStackFrames();
        if(frames.length == 0) return null;
        return frames[0];
    }

    public String getName() throws DebugException {
        return name;
    }

    public IBreakpoint[] getBreakpoints() {
        synchronized (breakpoints) {
            IBreakpoint[] bkps = new IBreakpoint[breakpoints.size()];
            return breakpoints.toArray(bkps);
        }
    }

    public String getModelIdentifier() {
        return GODBasePlugin.getDefault().getBundle().getSymbolicName();
    }

    public ILaunch getLaunch() {
        return this.getDebugTarget().getLaunch();
    }

    public Object getAdapter(Class adapter) {
        if (adapter.isAssignableFrom(this.getClass()))
            return this;

        return super.getAdapter(adapter);
    }

    public boolean canResume() {
        return this.isSuspended();
    }

    public boolean canSuspend() {
        return !this.isSuspended();
    }

    public boolean canStepInto() {
        try {
            return getLockedHead().canStepInto();
        } finally {
            rsLock.unlock();
        }
    }

    public boolean canStepOver() {
        try {
            return getLockedHead().canStepOver();
        } finally {
            rsLock.unlock();
        }
    }

    public boolean canStepReturn() {
        try {
            return getLockedHead().canStepReturn();
        } finally {
            rsLock.unlock();
        }
    }

    public void stepInto() throws DebugException {
        try {
            getLockedHead().stepInto();
        } finally {
            rsLock.unlock();
        }
    }

    public void stepOver() throws DebugException {
        try {
            getLockedHead().stepOver();
        } finally {
            rsLock.unlock();
        }
    }

    public void stepReturn() throws DebugException {
        try {
            getLockedHead().stepReturn();
        } finally {
            rsLock.unlock();
        }
    }

    public boolean canTerminate() {
        return false;
    }

    public void terminate() throws DebugException {
    }

    /**
     * A distributed thread is terminated when all of its composing threads are
     * terminated.
     */
    public boolean isTerminated() {
        return getMode() == TERMINATED;
    }

    protected int realTopFrame(VirtualStackframe vs, int position,
            int realLength) {
        /** Top virtual frame - hasn't left middleware code yet. */
        if (position == VIRTUALSTACK_TOPIDX)
            return TOP_INDEX;

        /** Has left middleware code, give back the top. */
        else
            return realLength - vs.getCallTop();
    }

    protected int realBaseFrame(VirtualStackframe vs, int position,
            int realLength) {
        int vslength = this.virtualStack().getVirtualFrameCount();

        /**
         * Frame is the first virtual frame. This means that its real base is
         * the actual real base (it has no base).
         */
        if (position == vslength)
            return realLength - 1;

        /** Otherwise we get the real index. */
        else
            return realLength - vs.getCallBase();
    }

    private void clearBreakpoints() {
        breakpoints.clear();
    }

    /*
     * ====================================================================== -
     * These methods are called by local threads when they have events to - -
     * report. -
     * ======================================================================
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.managing.tracker.IDistributedThread#hitByBreakpoint(org.eclipse.debug.core.model.IBreakpoint,
     *      ddproto1.debugger.managing.tracker.ILocalThread)
     */
    public void hitByBreakpoint(IBreakpoint bp, ILocalThread lt) {
        if (isStaleOrEarly(lt))
            return;
        if (pendingHandler.get() != null
                && pendingHandler.get().isTargetBreakpoint(bp)) {
            pendingHandler.set(null);
            // updateThreadBeforeHeadStatus();
            suspended(lt, DebugEvent.STEP_END);
        } else {
            suspended(lt, DebugEvent.BREAKPOINT);
            breakpoints.add(bp);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.managing.tracker.IDistributedThread#suspended(ddproto1.debugger.managing.tracker.ILocalThread)
     */
    public void suspended(ILocalThread lt, int detail) {
        synchronized (suspended) {
            if (isStaleOrEarly(lt))
                return;

            assert !suspended.contains(lt);
            suspended.add(lt);

            /**
             * If this is the first component thread to be suspended, we fire a
             * suspend event. If suspension is due to breakpoint or step end,
             * then the event must have happenned at the head thread. Otherwise
             * it doesn't.
             */
            if (shouldBeHeadEvent(detail) && !isHeadEvent(lt)) {
                markDamage(lt, "Caught event non-head thread. The thread "
                        + "should be suspended but it is not.", true);
            }

            setMode(SUSPENDED);

            if (suspended.size() == 1) {
                fireSuspendEvent(detail);
            } else {
                fireChangeEvent(DebugEvent.CONTENT);
            }
        }
    }

    private boolean shouldBeHeadEvent(int detail) {
        return (detail == DebugEvent.STEP_END)
                || (detail == DebugEvent.BREAKPOINT);
    }

    private void markDamage(ILocalThread lt, String reason, boolean fireEvent) {
        rsLock.lock();
        try {
            VirtualStackframe vs = virtualStack().frameOfThread(lt);
            if (vs == null) {
                logger.error("Caught error: <" + reason
                        + "> but frame was popped before it could be "
                        + "marked as damaged.");
            } else {
                vs.flagAsDamaged(reason);
            }
            if (fireEvent)
                fireChangeEvent(DebugEvent.CONTENT);
        } finally {
            rsLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.managing.tracker.IDistributedThread#resumed(ddproto1.debugger.managing.tracker.ILocalThread)
     */
    public void resumed(ILocalThread lt, int detail) {
        synchronized (suspended) {
            if (isStaleOrEarly(lt))
                return;
            assert suspended.contains(lt);
            suspended.remove(lt);

            // If it's the head thread that is stepping, then the
            // distributed thread is also stepping.
            if (isHeadEvent(lt)) {
                clearBreakpoints();
                if (detail == DebugEvent.STEP_INTO)
                    setMode(STEPPING);
            }

            // Checks if all threads have been resumed.
            if (suspended.size() == 0) {
                // Yes - distributed thread has been resumed.
                setMode(RUNNING);
                fireResumeEvent(detail);
            } else {
                // No - notify listeners of internal change.
                fireChangeEvent(DebugEvent.CONTENT);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.debugger.managing.tracker.IDistributedThread#died(ddproto1.debugger.managing.tracker.ILocalThread)
     */
    public void died(ILocalThread lt) {
        rsLock.lock();
        try {
            /** Only predictable case for now. */
            if (isHeadEvent(lt) && (virtualStack().length() == 1)) {
                setMode(TERMINATED);
                fDTM.notifyDeath(getId());
                fireTerminateEvent();
            } else {
                markDamage(lt, "Thread is dead.", true);
            }
        } finally {
            rsLock.unlock();
        }
    }

    /**
     * Tests whether a given notification is stale or early - i.e., it could be
     * that:
     * 
     * 1) A notification thread was already dispatching an event to this DT when
     * we called unbindFromParentDT. All events that happen-before
     * unbindFromParentDT should cause this method to return true.
     * 
     * 2) Hum... I forgot. (!!!)
     * 
     * @param source
     * @return
     */
    private boolean isStaleOrEarly(ILocalThread source) {
        synchronized (suspended) {
            if (!this.actualElements.contains(source))
                return true;
        }
        return false;
    }

    /**
     * Ensures that an event has occurred at the head thread. Returns false if
     * it hasn't.
     * 
     * @param lt
     * @return
     */
    private boolean isHeadEvent(ILocalThread lt) {
        try {
            ILocalThread head = this.getLockedHead();
            return lt.equals(head);
        } finally {
            rsLock.unlock();
        }
    }

    /**
     * Adds a thread to the internal suspension/component sets. This method
     * assumes that the virtual stack frame won't change while it's in progress.
     * It also assumes that the thread being removed is the head thread.
     * 
     * @param t
     */
    private void addHeadToTables(ILocalThread t, boolean isSuspended) {
        synchronized (suspended) {
            actualElements.add(t);
            if (isSuspended)
                this.suspended(t, DebugEvent.UNSPECIFIED);
        }
    }

    /**
     * Removes a thread from the internal suspension/component sets. This method
     * assumes that the virtual stack frame won't change while it's in progress.
     * It also assumes that the thread 't' passed as parameter is the head
     * thread.
     * 
     * @param t
     */
    private void removeHeadFromTables(ILocalThread t) throws DebugException {
        synchronized (suspended) {
            suspended.remove(t);
            actualElements.remove(t);

            ILocalThread newHead = getHead().getThreadReference();

            /**
             * A brief note on possible policies when the thread before the head
             * is suspended and the current head is being popped.
             * 
             * Policy 1: Resume it. It works well, but the user never gets a
             * chance to see middleware code.
             * 
             * Policy 2: Don't resume it, filter middleware frames in the
             * distributed thread and show all frames in the local thread. While
             * this is ideal, it is more cumbersome to implement because it'd
             * require tracking the size of the stack of local thread and
             * synchronizing it with the virtual stackframe that contains it at
             * each step request. Hum. Doesn't sound difficult as we already get
             * suspension notifications from the local threads. All I'd have to
             * do is:
             * 
             * onSuspend(LocalThread lt) vsf := virtual stackframe which
             * contains lt size := size of lt's call stack vsf.setCalltop(size);
             * 
             * Cool.
             * 
             * TODO Implement Policy 2.
             */

            // DT is suspended. GUI updates are required.
            if (suspended.size() > 0) {
                /**
                 * It doesn't matter if the thread is suspended or not. What
                 * matters is what we know about the thread.
                 */
                boolean isSuspended = suspended.contains(newHead);
                //            
                // if(isSuspended){
                // setMode(SUSPENDED);
                if (isSuspended) {
                    newHead.resume();
                    fireSuspendEvent((getMode() == STEPPING) ? DebugEvent.STEP_END
                            : DebugEvent.UNSPECIFIED);
                } else {
                    fireChangeEvent(DebugEvent.CONTENT);
                }
            }
        }
    }

    protected void beginRemoteStepping(int stepMode) {
        if (!pendingHandler.compareAndSet(null, new RemoteStepHandler()))
            throw new IllegalStateException(
                    "Pending step handler hasn't been cleared.");

        RemoteStepHandler rsh = pendingHandler.get();
        rsh.begin(stepMode);
    }

    protected void setServersideBreakpoint(IBreakpoint bkp) {
        if (pendingHandler.get() == null)
            throw new IllegalStateException(
                    "Protocol error. Step remote hasn't begun.");
        pendingHandler.get().setServerSideBreakpoint(bkp);
    }

    public class RemoteStepHandler {

        private volatile IBreakpoint fBkp;

        private boolean begin(int stepMode) {
            int currentState = state;
            if ((currentState & STEPPING) != 0)
                return false;
            setMode(STEPPING);
            fireResumeEvent(stepMode);
            return true;
        }

        public void setServerSideBreakpoint(IBreakpoint bkp) {
            fBkp = bkp;
        }

        protected boolean isTargetBreakpoint(IBreakpoint bkp) {
            return fBkp.equals(bkp);
        }
    }
}
