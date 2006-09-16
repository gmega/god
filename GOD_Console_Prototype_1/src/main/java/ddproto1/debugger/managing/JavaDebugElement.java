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
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.tracker.GODDebugElement;

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
    
    private IJavaDebugTarget fJavaDebugTarget;
    
    public JavaDebugElement(IJavaDebugTarget target){
        super(target);
        fJavaDebugTarget = target;
    }
    
    public IJavaDebugTarget getJavaDebugTarget(){
        return fJavaDebugTarget;
    }
    
    public String getModelIdentifier(){
        return IConfigurationConstants.JAVA_DEBUGGER_MODEL_ID;
    }
    
    protected void notSupported(String what) throws DebugException{
        requestFailed("Not supported - " + what, null);
    }
}
