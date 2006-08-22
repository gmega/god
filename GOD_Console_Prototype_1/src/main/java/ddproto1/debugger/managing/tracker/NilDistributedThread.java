/*
 * Created on 1/08/2006
 * 
 * file: NilDistributedThread.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.commons.UnsupportedException;

public class NilDistributedThread implements IDistributedThread {

    public void hitByBreakpoint(IBreakpoint bp, ILocalThread lt) {
    }

    public void beganStepping(ILocalThread lt, int detail) {
    }

    public void finishedStepping(ILocalThread lt) {
    }

    public void suspended(ILocalThread lt, int detail) {
    }

    public void resumed(ILocalThread lt, int detail) {
    }

    public void died(ILocalThread lt) {
    }

    public int getId() {
        throw new UnsupportedException();
    }

    public IStackFrame[] getStackFrames() throws DebugException {
        return new IStackFrame[0];
    }

    public boolean hasStackFrames() throws DebugException {
        return false;
    }

    public int getPriority() throws DebugException {
        return -1;
    }

    public IStackFrame getTopStackFrame() throws DebugException {
        GODBasePlugin.throwDebugException("Unsupported operation.");
        return null;
    }

    public String getName() throws DebugException {
        return "Nil distributed thread";
    }

    public IBreakpoint[] getBreakpoints() {
        return new IBreakpoint[0];
    }

    public String getModelIdentifier() {
        return GODBasePlugin.getDefault().getBundle().getSymbolicName();
    }

    public IDebugTarget getDebugTarget() {
        throw new UnsupportedOperationException();
    }

    public ILaunch getLaunch() {
        throw new UnsupportedOperationException();
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public boolean canResume() {
        return false;
    }

    public boolean canSuspend() {
        return false;
    }

    public boolean isSuspended() {
        return false;
    }

    public void resume() throws DebugException { }

    public void suspend() throws DebugException { }

    public boolean canStepInto() {
        return false;
    }

    public boolean canStepOver() {
        return false;
    }

    public boolean canStepReturn() {
        return false;
    }

    public boolean isStepping() {
        return false;
    }

    public void stepInto() throws DebugException { }

    public void stepOver() throws DebugException { }

    public void stepReturn() throws DebugException { }

    public boolean canTerminate() { return false; }

    public boolean isTerminated() {
        return false;
    }

    public void terminate() throws DebugException { }
}
