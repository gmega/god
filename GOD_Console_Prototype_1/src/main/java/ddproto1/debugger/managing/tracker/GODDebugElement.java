/*
 * Created on 16/05/2006
 * 
 * file: AbstractDebugElement.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;

public abstract class GODDebugElement extends DebugElement{
    
    public GODDebugElement(IDebugTarget tg){
        super(tg);
    }
    
    protected void requestFailed(String reason, Exception cause) throws DebugException{
        throw new DebugException(new Status(IStatus.ERROR, GODBasePlugin.getDefault().getBundle().getSymbolicName(), 
                IStatus.ERROR, reason, cause));
    }
    
    public String getModelIdentifier(){
        return GODBasePlugin.getDefault().getBundle().getSymbolicName();
    }
}
