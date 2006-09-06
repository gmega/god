/*
 * Created on 24/08/2006
 * 
 * file: LocalStackframeWrapper.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

public class LocalStackframeWrapper extends DebugElement implements IStackFrame {

    private final IStackFrame fLocalFrame;
    private final IThread fParentDT;
    
    public LocalStackframeWrapper(IThread parentDT, IStackFrame localFrame, IDebugTarget parent){
        super(parent);
        fLocalFrame = localFrame;
        fParentDT = parentDT;
    }
    
    public IThread getThread() {
        return fParentDT;
    }

    public IVariable[] getVariables() throws DebugException {
        return fLocalFrame.getVariables();
    }

    public boolean hasVariables() throws DebugException {
        return fLocalFrame.hasVariables();
    }

    public int getLineNumber() throws DebugException {
        return fLocalFrame.getLineNumber();
    }

    public int getCharStart() throws DebugException {
        return fLocalFrame.getCharStart();
    }

    public int getCharEnd() throws DebugException {
        return fLocalFrame.getCharEnd();
    }

    public String getName() throws DebugException {
        return fLocalFrame.getName();
    }

    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return fLocalFrame.getRegisterGroups();
    }

    public boolean hasRegisterGroups() throws DebugException {
        return fLocalFrame.hasRegisterGroups();
    }

    public String getModelIdentifier() {
        return fLocalFrame.getModelIdentifier();
    }

    public boolean canStepInto() {
        return fLocalFrame.canStepInto();
    }

    public boolean canStepOver() {
        return fLocalFrame.canStepOver();
    }

    public boolean canStepReturn() {
        return fLocalFrame.canStepReturn();
    }

    public boolean isStepping() {
        return fLocalFrame.isStepping();
    }

    public void stepInto() throws DebugException {
        fLocalFrame.stepInto();
    }

    public void stepOver() throws DebugException {
        fLocalFrame.stepOver();
    }

    public void stepReturn() throws DebugException {
        fLocalFrame.stepReturn();
    }

    public boolean canResume() {
        return fLocalFrame.canResume();
    }

    public boolean canSuspend() {
        return fLocalFrame.canSuspend();
    }

    public boolean isSuspended() {
        return fLocalFrame.isSuspended();
    }

    public void resume() throws DebugException {
        fLocalFrame.resume();
    }

    public void suspend() throws DebugException {
        fLocalFrame.suspend();
    }

    public boolean canTerminate() {
        return fLocalFrame.canTerminate();
    }

    public boolean isTerminated() {
        return fLocalFrame.isTerminated();
    }

    public void terminate() throws DebugException {
        fLocalFrame.terminate();
    }

    @Override
    public Object getAdapter(Class adaptee){
        if(adaptee.isAssignableFrom(this.getClass())) return this;
        else{
            Object adapter = 
                super.getAdapter(adaptee);
            if(adapter != null)
                return adapter;
        }
        
        return fLocalFrame.getAdapter(adaptee);
    }
}
