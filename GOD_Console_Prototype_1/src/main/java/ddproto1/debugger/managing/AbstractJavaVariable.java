/*
 * Created on Nov 3, 2005
 * 
 * file: JavaVariable.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import ddproto1.commons.DebuggerConstants;


/**
 * Base functionality for all Java variable classes. 
 * 
 * 
 * @author giuliano
 *
 */
public abstract class AbstractJavaVariable extends JavaDebugElement implements IVariable{

    private LocalVariable vDelegate;
    private JavaStackframe parent;
    private IValue cachedValue = null;
    
    protected AbstractJavaVariable(JavaStackframe parentFrame){
        super(parentFrame.getJavaDebugTarget());
        this.parent = parentFrame;
    }
    
    public IValue getValue() throws DebugException {
        if(cachedValue != null)
            cachedValue = this.getValueInternal();
        return cachedValue;
    }

    public String getName() throws DebugException {
        return vDelegate.name();
    }

    public String getReferenceTypeName() throws DebugException {
        IValue val = this.getValue();
        return val.getReferenceTypeName();
    }
    
    protected abstract IValue getValueInternal() throws DebugException;

    public boolean hasValueChanged() throws DebugException {
        return false;
    }
    
    public void setValue(IValue val) throws DebugException{
        this.requestFailed("This variable doesn't support value modification.", null);
    }

    public boolean verifyValue(String expression) throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean verifyValue(IValue value) throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

}
