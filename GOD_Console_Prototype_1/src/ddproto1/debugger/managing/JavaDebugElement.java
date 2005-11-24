/*
 * Created on Nov 4, 2005
 * 
 * file: JavaDebugElement.java
 */
package ddproto1.debugger.managing;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

import ddproto1.commons.DebuggerConstants;

/**
 * Base Java debug element. I'm making an abstract superclass because the JDT
 * people did it like this, so I'll probably feel the need to have an abstract
 * superclass one day.
 * 
 * 
 * @author giuliano
 *
 */
public abstract class JavaDebugElement extends DebugElement{
    
    private IJavaDebugTarget jdTarget;
    
    public JavaDebugElement(IJavaDebugTarget target){
        super(target);
    }
    
    public IJavaDebugTarget getJavaDebugTarget(){
        return jdTarget;
    }
    
    public void requestFailed(String reason, Exception cause) throws DebugException{
        throw new DebugException(new Status(IStatus.ERROR, DebuggerConstants.PLUGIN_ID, 
                IStatus.ERROR, reason, cause));
    }
    
    public void notSupported(String what) throws DebugException{
        requestFailed("Not supported - " + what, null);
    }
    
    public String getModelIdentifier(){
        return DebuggerConstants.PLUGIN_ID;
    }
}
