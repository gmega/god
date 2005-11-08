/*
 * Created on Nov 5, 2005
 * 
 * file: JavaThisVariable.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;

import com.sun.jdi.ObjectReference;

public class JavaThisVariable extends AbstractJavaVariable {
    
    private ObjectReference thisPtr;

    protected JavaThisVariable(IStackFrame parentFrame, ObjectReference thisPtr) {
        super(parentFrame);
        this.thisPtr = thisPtr;
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
