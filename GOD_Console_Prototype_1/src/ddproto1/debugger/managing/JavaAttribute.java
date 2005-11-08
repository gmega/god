/*
 * Created on Nov 5, 2005
 * 
 * file: Attribute.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;

import com.sun.jdi.Field;

public class JavaAttribute extends ModifiableVariable {
    
    private Field fDelegate;
    private IStackFrame frame;
    
    public JavaAttribute(IStackFrame parent, Field f){
        super(parent);
        this.fDelegate = f;
        this.frame = parent;
    }

    @Override
    protected IValue getValueInternal() throws DebugException { return null; }

    public void setValue(String expression) throws DebugException { }
}
