/*
 * Created on Nov 5, 2005
 * 
 * file: JavaLocalVariable.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;

import com.sun.jdi.LocalVariable;

public class JavaLocalVariable extends AbstractJavaVariable{

    private LocalVariable vDelegate;
    
    protected JavaLocalVariable(IStackFrame parentFrame, LocalVariable var) {
        super(parentFrame);
        this.vDelegate = var;
    }

    @Override
    protected IValue getValueInternal() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setValue(String expression) throws DebugException {
        // TODO Auto-generated method stub
        
    }

    public boolean supportsValueModification() {
        // TODO Auto-generated method stub
        return false;
    }
    
}
