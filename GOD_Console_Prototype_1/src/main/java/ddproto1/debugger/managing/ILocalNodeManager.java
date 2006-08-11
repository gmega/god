/*
 * Created on Jun 5, 2006
 * 
 * file: INodeManager.java
 */
package ddproto1.debugger.managing;

import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.commons.Event;

/**
 * Language-neutral node manager interface. It kind of clashes with 
 * IDebugTarget, but has some more methods. 
 * 
 * @author giuliano
 *
 */
public interface ILocalNodeManager extends IAdaptable, IConfigurable, INodeManager{

    /**
     * Informs whether the remote target connects back to a server or not. 
     * This means that prepareForConnecting must be called before launching 
     * the target.
     * 
     * 
     *  Target                    Server (us)
     *                               O<---------- prepareForConnecting()
     *          <launch target>      |<---------- launch()
     *    O<-------------------------|
     *    |  <target connects back>  |
     *    |------------------------->|
     *    |                          |
     * 
     */
    public boolean connectsBack();
    
    /**
     * If the remote target connects back, then this node requires that 
     * prepareForConnecting be called before the remote process is launched.
     * 
     */
    public void prepareForConnecting() throws DebugException;

    /** Connects to a previously launched remote debug target. The launch that
     * contains the process as well as a reference to the process itself are
     * given as parameters.
     * 
     * @throws IllegalAttributeException - if there are issues with the node spec configuration.
     * @throws DebugException - if an error occurs while connecting.
     * @throws NestedRuntimeException if an error occurs during connection.
     */
    public void connect() throws IllegalAttributeException, DebugException;
    
    /**
     * Returns the deferrable request queue for this node manager.
     * 
     * @return A DeferrableRequestQueue. 
     */
    public DeferrableRequestQueue getDeferrableRequestQueue();
    
    /**
     * Returns the globally unique identifier in string form for
     * this node.
     * 
     * @return
     */
    public String getGID();
    
    /**
     * Returns the language-independent thread manager for this node manager.
     * 
     * @return
     */
    public ILocalThreadManager getThreadManager();
    
    /**
     * Sets the process that represents this ILocalNodeManager. Should be
     * called only once.
     * 
     * @throws IllegalStateException if setProcess is called more than once.
     * @param process
     */
    public void setProcess(IProcess process);
    
    /**
     * Returns the IProcess that represents this node manager, or nil if 
     * no process has yet been set.
     * 
     * @return
     */
    public IProcess getProcess();
    
    /** 
     * Sets a breakpoint from a DDWP event generated at a local agent whose
     * target runtime matches the target runtime of this node manager. 
     * 
     * @param evt 
     * @return
     */
    public IBreakpoint setBreakpointFromEvent(Event evt) throws DebugException;
}
