/*
 * Created on Nov 3, 2005
 * 
 * file: JavaStackframe.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;

import ddproto1.commons.DebuggerConstants;
import ddproto1.util.MessageHandler;

/**
 * 
 * @author giuliano
 *
 */
public class JavaStackframe extends JavaDebugElement implements IStackFrame{

    private static final Logger logger = MessageHandler.getInstance().getLogger(JavaStackframe.class);
    
    private IThread fParentThread;
    private StackFrame fJDIStackframe;
    
    private IVariable [] fCachedVars;
    
    public JavaStackframe(JavaThread parent, StackFrame sfDelegate){
        super(parent.getJavaDebugTarget());
        this.fJDIStackframe = sfDelegate;
        this.fParentThread = parent;
    }
    
    public IThread getThread() {
        return fParentThread;
    }

    public IVariable[] getVariables() throws DebugException {

        if(fCachedVars != null) return fCachedVars;
        
        /** Variables = local variables + this. */
        List<AbstractJavaVariable> varList = new ArrayList<AbstractJavaVariable>();
        Location loc = this.getLocation();
        Method enclosing = loc.method();
        
        /** Is this method static? */
        if (enclosing.isStatic()) {
            try {
                /** It's static. Let's add all class fields. */
                List<Field> classFields = enclosing.declaringType().allFields();
                for (Field classField : classFields)
                    varList.add(new JavaAttribute(this, classField));
            } catch (ClassNotPreparedException ex) {
                this.requestFailed("Error while retrieving class fields.", ex);
            }
        } else {
            /**
             * Not static. Let's add a pointer to the 'this' variable.
             */
            varList.add(new JavaThisVariable(this, getJDIStackframe().thisObject()));
        }

        /** Now we squeeze in the locals. */
        try {
            List<LocalVariable> localVars = getJDIStackframe().visibleVariables();
            for (LocalVariable localVar : localVars)
                varList.add(new JavaLocalVariable(this, localVar));
        } catch (AbsentInformationException ex) {
            this.requestFailed("Error while retrieving local variables.", ex);
        }

        IVariable[] _varList = new IVariable[varList.size()];
        fCachedVars = varList.toArray(_varList);
        return fCachedVars;
    }

    public boolean hasVariables() throws DebugException {
        return this.getVariables().length != 0;
    }

    public int getLineNumber() throws DebugException {
        Location loc = this.getLocation();
        return loc.lineNumber();
    }
    
    protected Location getLocation() throws DebugException{
        try{
            return getJDIStackframe().location();
        }catch(InvalidStackFrameException ex){
            this.requestFailed("Error while inspecting stack frame.", ex);
            return null; // Shuts up the compiler.
        }
    }

    public int getCharStart() throws DebugException {
        /** The JPDA doesn't support this. */
        return -1;
    }

    public int getCharEnd() throws DebugException {
        /** JPDA doesn't support this. */
        return -1;
    }

    public String getName() throws DebugException {
        StringBuffer name = new StringBuffer();
        Method m = this.getLocation().method();
        renderMethodNameOn(name, m);
        renderParameterListOn(name, m);
        renderLocationOn(name, this.getLocation());
        return name.toString();
    }
    
    private void renderMethodNameOn(StringBuffer target, Method m){
        ReferenceType rt = m.location().declaringType();
        target.append(rt.name());
        target.append('.');
        target.append(m.name());
    }
    
    private void renderParameterListOn(StringBuffer target, Method m){
        target.append('(');
        List<String> argNames = (List<String>)m.argumentTypeNames();
        for(String paramName : argNames){
            target.append(paramName);
            target.append(',');
        }
        if(argNames.isEmpty())
            target.append(')');
        else
            target.setCharAt(target.length()-1, ')');

    }
    
    private void renderLocationOn(StringBuffer target, Location myLocation){
        target.append(" :: ");
        target.append(myLocation.lineNumber());
    }

    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        /** Register groups??? No, thanks. */
        return new IRegisterGroup[0];
    }

    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    public String getModelIdentifier() {
        return DebuggerConstants.PLUGIN_ID;
    }

    public boolean canStepInto()  {
        return this.isTopFrame();
    }
    
    private boolean isTopFrame(){
        try{
            /** Only top frames are allowed. It doesn't make any sense
             * to allow non-top frames since they're already "going into". */
            IStackFrame sFrame = fParentThread.getTopStackFrame();
            return sFrame == this; 
        }catch(DebugException ex){
            logger.error("Failed to acquire top stack frame.", ex);
            return false;
        }
    }

    public boolean canStepOver() {
        // TODO Allow non-top frames to step over.
        return this.isTopFrame();
    }

    public boolean canStepReturn() {
        // TODO Allow non-top frames to step return.
        return this.isTopFrame();
    }

    public boolean isStepping() {
        return fParentThread.isStepping();
    }
    
    private void clearVarCache(){
        fCachedVars = null;
    }

    public void stepInto() throws DebugException {
        if(!canStepInto()) this.requestFailed("This stack frame cannot step into.", null); 
        fParentThread.stepInto();
        this.clearVarCache();
    }

    public void stepOver() throws DebugException {
        if(!canStepOver()) this.requestFailed("This stack frame cannot step over.", null);
        fParentThread.stepOver();
        this.clearVarCache();
    }

    public void stepReturn() throws DebugException {
        if(!canStepInto()) this.requestFailed("This stack frame cannot step return.", null);
        fParentThread.stepReturn();
        this.clearVarCache();
    }

    public boolean canResume() {
        return fParentThread.canResume();
    }

    public boolean canSuspend() {
        return fParentThread.canSuspend();
    }

    public boolean isSuspended() {
        return fParentThread.isSuspended();
    }

    public void resume() throws DebugException {
        if(!canResume()) this.requestFailed("Cannot resume this stack frame.", null);
        fParentThread.resume();
    }

    public void suspend() throws DebugException {
        if(!canSuspend()) this.requestFailed("Cannot suspend this stack frame.", null);
        fParentThread.suspend();
    }

    public boolean canTerminate() {
        return fParentThread.canTerminate();
    }

    public boolean isTerminated() {
        return fParentThread.isTerminated();
    }

    public void terminate() throws DebugException {
        if(!fParentThread.canTerminate()) this.requestFailed("Cannot terminate.", null);
        fParentThread.terminate();
    }
    
    private StackFrame getJDIStackframe(){
        return fJDIStackframe;
    }

    /** Two "Eclipse model frames" are equivalent
     * if their underlying frames are also equivalent.
     */
    public boolean equals(Object another){
        if(!(another instanceof JavaStackframe))
            return false;
        JavaStackframe other = (JavaStackframe)another;
        return other.getJDIStackframe().equals(this.getJDIStackframe());
    }
    
    public int hashCode(){
        return getJDIStackframe().hashCode();
    }

}
