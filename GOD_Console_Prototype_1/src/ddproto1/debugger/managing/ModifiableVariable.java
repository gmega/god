/*
 * Created on Nov 5, 2005
 * 
 * file: ModifiableVariable.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.model.IStackFrame;


public abstract class ModifiableVariable extends AbstractJavaVariable {
    
    protected ModifiableVariable(IStackFrame is) { super(is); }
    
    public boolean supportsValueModification(){
        return true;
    }
    

}
