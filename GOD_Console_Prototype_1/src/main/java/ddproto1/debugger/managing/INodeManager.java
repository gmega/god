/*
 * Created on 25/07/2006
 * 
 * file: INodeManager.java
 */
package ddproto1.debugger.managing;

import org.eclipse.debug.core.model.IStackFrame;

public interface INodeManager {
    
    public static final IStackFrame[] NO_CALLSTACK = new IStackFrame[0];
    
    public String getName();
    public IThreadManager getThreadManager();
}
