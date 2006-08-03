/*
 * Created on 16/05/2006
 * 
 * file: ThreadStackFrame.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

public class ThreadStackFrame implements IStackFrame {

    public IThread fThread;
    public IThread fParentThread;
    
    public ThreadStackFrame(IThread component, IThread parent){
        this.fThread = component;
        this.fParentThread = parent;
    }
    
    public IThread getThread() { return fParentThread; }

    public IVariable[] getVariables() throws DebugException { return new IVariable [] { }; }

    public boolean hasVariables() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    public int getLineNumber() throws DebugException {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getCharStart() throws DebugException {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getCharEnd() throws DebugException {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getName() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasRegisterGroups() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    public String getModelIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    public IDebugTarget getDebugTarget() {
        // TODO Auto-generated method stub
        return null;
    }

    public ILaunch getLaunch() {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getAdapter(Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean canStepInto() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canStepOver() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canStepReturn() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStepping() {
        // TODO Auto-generated method stub
        return false;
    }

    public void stepInto() throws DebugException {
        // TODO Auto-generated method stub

    }

    public void stepOver() throws DebugException {
        // TODO Auto-generated method stub

    }

    public void stepReturn() throws DebugException {
        // TODO Auto-generated method stub

    }

    public boolean canResume() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canSuspend() { 
        if(fThread != null) return fThread.canSuspend(); 
        return false;
    }

    public boolean isSuspended() {
        if(fThread != null) return fThread.isSuspended();
        return false;
    }

    public void resume() throws DebugException {
    }

    public void suspend() throws DebugException {
        // TODO Auto-generated method stub

    }

    public boolean canTerminate() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    public void terminate() throws DebugException {
        // TODO Auto-generated method stub

    }

}
