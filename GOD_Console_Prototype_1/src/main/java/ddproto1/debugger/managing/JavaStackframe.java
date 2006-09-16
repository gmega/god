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
import com.sun.jdi.VMDisconnectedException;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.util.MessageHandler;

/**
 * 
 * @author giuliano
 *
 */
public class JavaStackframe extends JavaDebugElement implements IStackFrame{

    private static final Logger logger = MessageHandler.getInstance().getLogger(JavaStackframe.class);
    private static final Logger staleframeLogger = MessageHandler.getInstance().getLogger(JavaStackframe.class.getName() + ".staleframeLogger");
    
    private final JavaThread fParentThread;
        
    private volatile boolean fValid = true;
    
    private IVariable [] fCachedVars;
    
    public JavaStackframe(JavaThread parent){
        super(parent.getJavaDebugTarget());
        fParentThread = parent;
    }
    
    public IThread getThread() {
        return getParentThread();
    }

    public IVariable[] getVariables() throws DebugException {

        if(fCachedVars != null) return fCachedVars;
        if(!isValid()) return new IVariable[0];
        
        /** Variables = local variables + this. */
        List<AbstractJavaVariable> varList = new ArrayList<AbstractJavaVariable>();
        Method enclosing = getLocation().method();
        
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
        return isValid() && this.getVariables().length != 0;
    }

    public int getLineNumber() throws DebugException {
        Location loc = this.getLocation();
        if(loc == null) return -1;
        return loc.lineNumber();
    }
    
    /**
     * Returns the JDI location object associated to this JavaStackframe,
     * or null if this stack frame is no longer valid. 
     * 
     * @return
     * @throws DebugException
     */
    public Location getLocation() throws DebugException{
        try{
            StackFrame sFrame = getJDIStackframe();
            if(sFrame == null) return null;
            return sFrame.location();
        }catch(InvalidStackFrameException ex){
            if(staleframeLogger.isDebugEnabled())
                staleframeLogger.debug("Stale frame: " + getJDIStackframe().toString());
            GODBasePlugin.throwDebugExceptionWithErrorAndStatus("Frame no longer valid.", ex, DebuggerConstants.LOCAL_THREAD_RESUMED);
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
        try{
            if(!isValid()) return "<invalid>";
            StringBuffer name = new StringBuffer();
            Location loc = getLocation();
            Method m = loc.method();
            renderMethodNameOn(name, m);
            renderParameterListOn(name, m);
            renderLocationOn(name, loc);
            return name.toString();
        }catch(VMDisconnectedException ex){
            return "<disconnected>";
        }
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

    public boolean canStepInto()  {
        return this.isTopFrame();
    }
    
    private boolean isTopFrame(){
        try{
            if(!isValid()) return false;
            /** Only top frames are allowed. It doesn't make any sense
             * to allow non-top frames since they're already "going into". */
            IStackFrame sFrame = getParentThread().getTopStackFrame();
            return sFrame == this; 
        }catch(DebugException ex){
            logger.error("Failed to acquire top stack frame.", ex);
            return false;
        }
    }
    
    public boolean isValid(){
        return fValid;
    }
    
    public void invalidate(){
        fValid = false;
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
        return getParentThread().isStepping();
    }
    
    private void clearVarCache(){
        fCachedVars = null;
    }

    public void stepInto() throws DebugException {
        if(!canStepInto()) this.requestFailed("This stack frame cannot step into.", null); 
        getParentThread().stepInto();
        this.clearVarCache();
    }

    public void stepOver() throws DebugException {
        if(!canStepOver()) this.requestFailed("This stack frame cannot step over.", null);
        getParentThread().stepOver();
        this.clearVarCache();
    }

    public void stepReturn() throws DebugException {
        if(!canStepInto()) this.requestFailed("This stack frame cannot step return.", null);
        getParentThread().stepReturn();
        this.clearVarCache();
    }

    public boolean canResume() {
        return getParentThread().canResume();
    }

    public boolean canSuspend() {
        return getParentThread().canSuspend();
    }

    public boolean isSuspended() {
        return getParentThread().isSuspended();
    }

    public void resume() throws DebugException {
        if(!canResume()) this.requestFailed("Cannot resume this stack frame.", null);
        getParentThread().resume();
    }

    public void suspend() throws DebugException {
        if(!canSuspend()) this.requestFailed("Cannot suspend this stack frame.", null);
        getParentThread().suspend();
    }

    public boolean canTerminate() {
        return getParentThread().canTerminate();
    }

    public boolean isTerminated() {
        return getParentThread().isTerminated();
    }

    public void terminate() throws DebugException {
        if(!getParentThread().canTerminate()) this.requestFailed("Cannot terminate.", null);
        getParentThread().terminate();
    }
    
    private StackFrame getJDIStackframe()
        throws DebugException
    {
        return getParentThread().getJDIStackFrame(this);
    }

    private JavaThread getParentThread() {
        return fParentThread;
    }
    

// Equality comparison cannot be made when thread is resumed,
// and the platform doesn't guarantee it won't call stack frame
// methods once the thread has been resumed.
    
//    /** Two "Eclipse model frames" are equivalent
//     * if their underlying frames are also equivalent.
//     */
//    public boolean equals(Object another){
//        if(!(another instanceof JavaStackframe))
//            return false;
//        JavaStackframe other = (JavaStackframe)another;
//        return other.getJDIStackframe().equals(this.getJDIStackframe());
//    }
//    
//    public int hashCode(){
//        return fHashCode;
//    }

}
