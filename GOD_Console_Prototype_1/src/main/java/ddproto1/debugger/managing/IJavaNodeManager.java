/*
 * Created on May 9, 2006
 * 
 * file: IJavaNodeManager.java
 */
package ddproto1.debugger.managing;

import com.sun.jdi.Mirror;

import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IVotingManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.exception.TargetRequestFailedException;
import ddproto1.sourcemapper.ISourceMapper;

/**
 * Specialized node manager for Java (tm) programs based on our 
 * debugger.
 * 
 * @author giuliano
 */
public interface IJavaNodeManager extends Mirror, ILocalNodeManager{

    /**
     * Terminates the current node manager.
     */
    public void terminate();

    /**
     * Disconnects the current node manager.
     */
    public void disconnect();

    public boolean isAvailable();

    public boolean isDisconnecting();

    public boolean isTerminating();

    public boolean isTerminated();

    /** Determines with <b>reasonable</b> accuracy wether there's a 
     * valid connection to the associated JVM or not.
     * 
     * @return <b>true</b> if a connection exists for sure or <b>false</b>
     * if <i>perhaps</i> it doesn't exist.
     */
    public boolean isConnected();

    /** Returns the ThreadManager for this VirtualMachine.
     * 
     * 
     * @return ThreadManager
     * @see ddproto1.debugger.managing.ThreadManager 
     */
    public IJavaThreadManager getThreadManager();

    /** Returns the EventManager for this VirtualMachine.
     * 
     * @see ddproto1.debugger.eventhandler.IEventManager
     */
    public IEventManager getEventManager();

    public ISourceMapper getSourceMapper();

    public EventDispatcher getDispatcher();

    public IVotingManager getVotingManager();

    /** Returns the name of this node, extracted from the NodeInfo passed
     * on at the constructor.
     * 
     */
    public void setApplicationExceptionDetector(AbstractEventProcessor aep);

    public boolean isSuspended();

    public void suspend() throws TargetRequestFailedException;

    public void resume() throws TargetRequestFailedException;
    
    public IJavaDebugTarget getDebugTarget() throws IllegalStateException;

}