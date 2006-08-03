/*
 * Created on Nov 30, 2005
 * 
 * file: GlobalAgent.java
 */
package ddproto1.debugger.managing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import ddproto1.GODBasePlugin;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.PropertyHandler;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.commons.IQueriableConfigurable;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.server.SeparatingHandler;
import ddproto1.debugger.server.SocketServer;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * This class represents the Global Agent. It does a wrapping of all controls required
 * to operate the abstract entity "Global Agent", including controls to start/stop it and
 * retrieving threads, breakpoints, etcetera. 
 * 
 * @author giuliano
 *
 */
public class GlobalAgent extends DebugElement implements IDebugTarget, INodeManager, IQueriableConfigurable, IConfigurationConstants{
    
    private static Logger logger = 
        MessageHandler.getInstance().getLogger(GlobalAgent.class);
    
    private static final Set<String> attSet = Collections.unmodifiableSet(
            ConversionUtil.getInstance().toSet(
                    new String[] {CDWP_PORT,
                            MAX_QUEUE_LENGTH,
                            THREAD_POOL_SIZE,
                            GLOBAL_AGENT_ADDRESS}));
    
    private volatile DistributedThreadManager dtm;
    private SocketServer ddwpServer;
    private volatile boolean started = false;
    private volatile boolean starting = false;
    
    private PropertyHandler propertyServer = new PropertyHandler();
    
    private List<IDebugTarget> activeTargets;
    
    private final Map<String,String> attributes = 
        Collections.synchronizedMap(new HashMap<String, String>());
    
    public GlobalAgent(IObjectSpec spec) throws AttributeAccessException{
        super(null);
        setAttribute(GLOBAL_AGENT_ADDRESS, 
                spec.getAttribute(GLOBAL_AGENT_ADDRESS));
        setAttribute(MAX_QUEUE_LENGTH,
                spec.getAttribute(MAX_QUEUE_LENGTH));
        setAttribute(THREAD_POOL_SIZE, 
                spec.getAttribute(THREAD_POOL_SIZE));
        setAttribute(CDWP_PORT,
                spec.getAttribute(CDWP_PORT));
    }
    
    /**
     * This method should be called to start the global agent.
     * 
     * @param monitor
     * @throws CoreException
     */
    public void start(IProgressMonitor monitor) throws CoreException{
        synchronized(this){
            if(starting | started)
                return;
            starting = true;
        }

        try{
            monitor.beginTask("Starting Global Agent...", 1);
            init();
            startDDWPServer();
            if(monitor != null) monitor.worked(1);
        }catch(Exception ex){
            /** Error, shuts down all services. */
        	stopDDWPServer();
        	String failure = "Failed to start the global agent. ";
            logger.error(failure, ex);
            starting = false;
            GODBasePlugin.throwCoreExceptionWithError(failure, ex);
        }
        
        started = true;
        starting = false;
        this.fireCreationEvent();
    }
    
    public IThreadManager getThreadManager(){
        if(dtm == null)
            throw new IllegalStateException("Thread manager isn't ready.");
        
        return dtm;
    }
    
    protected void init(){
        activeTargets = new ArrayList<IDebugTarget>();
        dtm = new DistributedThreadManager(this);
    }
    
    public void stop(IProgressMonitor monitor){
        synchronized(this){
            if(!started) return;
            started = true;
        }
        /** One day we'll support reconnection, but not now. */
        killActiveProcesses();
        stopDDWPServer();
    }
    
    public synchronized void addTarget(IDebugTarget target){
        if(!isRunning()) throw new IllegalStateException("Cannot add targets while not running.");
        dtm.registerNode((ILocalNodeManager)target.getAdapter(ILocalNodeManager.class));
        activeTargets.add(target);
    }
    
    public synchronized void cancelTarget(IJavaDebugTarget target){
        activeTargets.remove(target);
    }
    
    private void startDDWPServer() throws CoreException{
        SocketServer ddwpServer = getServer();
        try{
            int max_threads = Integer.parseInt(getAttribute(THREAD_POOL_SIZE));
            int conn_queue_length = Integer.parseInt(getAttribute(MAX_QUEUE_LENGTH));
            int port = Integer.parseInt(getAttribute(CDWP_PORT));
            
            ddwpServer.setMaxThreads(max_threads);
            ddwpServer.setMaxConnections(conn_queue_length);
            ddwpServer.setPort(port);
            
            SeparatingHandler distributor = 
                new SeparatingHandler(DebuggerConstants.STATUS_FIELD_OFFSET);
            
            try{
            	if(ddwpServer.hasSingletonHandler()) 
            		ddwpServer.clearSingletonHandler();
            		
                ddwpServer.setSingletonHandler(distributor);
            }catch(ConfigException ex){
            		GODBasePlugin.
                    throwCoreExceptionWithError("Error while intializing DDWP server.", ex);
            }
            distributor.registerHandler(DebuggerConstants.NOTIFICATION, dtm);
            distributor.registerHandler(DebuggerConstants.REQUEST, propertyServer);
            
            ddwpServer.start();
        }catch(NumberFormatException ex){
            GODBasePlugin.throwDebugExceptionWithError("Error while parsing global agent parameters - not a number. Check your preferences.", ex);
        }catch (Exception ex){
        		GODBasePlugin.throwDebugExceptionWithError("Failed to start the global agent.", ex);
        }
    }
    
    protected synchronized void killActiveProcesses()
    {
        for(IDebugTarget target : activeTargets){
            try{
                if(target.canTerminate()) target.terminate();
            }catch(DebugException ex){
                logger.error("Could not terminate an active target. ", ex);
            }
        }
    }
    
    private void stopDDWPServer(){
        ddwpServer.stop();
    }
    
    private SocketServer getServer(){
        if(ddwpServer == null)
            ddwpServer =  new SocketServer();
        
        return ddwpServer;
    }
    
    
    public boolean isRunning(){
        return started;
    }
    
    public IProcess getProcess() {
        // TODO Auto-generated method stub
        return null;
    }

    public IThread[] getThreads() throws DebugException {
        return getThreadManager().getThreads();
    }

    public boolean hasThreads() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    public String getName(){
        return IConfigurationConstants.CENTRAL_AGENT_CONFIG_NAME;
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        // TODO Auto-generated method stub
        return false;
    }

    public String getModelIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean canTerminate() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    public void terminate() throws DebugException {
        // TODO Auto-generated method stub
        
    }

    public boolean canResume() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canSuspend() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSuspended() {
        // TODO Auto-generated method stub
        return false;
    }

    public void resume() throws DebugException {
        // TODO Auto-generated method stub
        
    }

    public void suspend() throws DebugException {
        // TODO Auto-generated method stub
        
    }

    public void breakpointAdded(IBreakpoint breakpoint) {
        // TODO Auto-generated method stub
        
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        // TODO Auto-generated method stub
        
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
        // TODO Auto-generated method stub
        
    }

    public boolean canDisconnect() {
        // TODO Auto-generated method stub
        return false;
    }

    public void disconnect() throws DebugException {
        // TODO Auto-generated method stub
        
    }

    public boolean isDisconnected() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsStorageRetrieval() { return false; }

    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    		GODBasePlugin.throwDebugExceptionWithError("This operation is not supported.", null);
    		return null; // never happens
    }

	public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
        if(!attributes.containsKey(key))
            throw new UninitializedAttributeException(key);
        return attributes.get(key);
	}

	public void setAttribute(String key, String val) 
		throws IllegalAttributeException, InvalidAttributeValueException {
        if(!getAttributeKeys().contains(key))
            throw new IllegalAttributeException(key);
		attributes.put(key, val);
	}

    public Set<String> getAttributeKeys() {
        return attSet;
    }

    public boolean isWritable() {
        return true;
    }
}
