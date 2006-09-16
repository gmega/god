/*
 * Created on Jun 26, 2006
 * 
 * file: ProcessServerManager.java
 */
package ddproto1.launcher.procserver;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.commons.IQueriableConfigurable;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.exception.ConfigException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.IJVMCommandLine;
import ddproto1.util.DelayedResult;
import ddproto1.util.IServiceLifecycle;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * This class is responsible for managing access to the various process control
 * servers that might be needed by the central agent. It encapsulates the protocol 
 * for launching and connecting to the process control server instances. 
 * 
 * 
 * --- No longer true ---
 * It is currently coupled to the Sun JVM via the 'renderCommandLine' private method
 * (which renders a Sun JVM compatible command line) but that could be easily changed
 * by providing a pluggable renderer.
 * ---------------------- 
 * 
 * This class should be thread-safe, and it doesn't support client-side locking.
 * 
 * TODO: Implement an IProcessServer wrapper that's aware of shutdown and synchronizes
 * clients. Currently our implementation will cause a NoSuchObjectException to be thrown 
 * if a client attempts to acquire a reference to a dying (but not yet dead) server.
 * That will confuse the recovery mechanism and it will abort with an exception. 
 * Work around: if NoSuchObjectException is thrown, stall for a while and try to acquire 
 * a reference again. 
 * 
 * @author giuliano
 *
 */
public class ProcessServerManager implements IConfigurationConstants, 
											IQueriableConfigurable, IProcessServerManager{

	private static final int CALLBACK_TIMEOUT = 20000;
    private static final int DEATH_POLL = 100;
	
    private static final List<IProcessEventListener> EMPTY_LIST = 
		new ArrayList<IProcessEventListener>(); 
	
	private static final Logger logger = MessageHandler.getInstance().getLogger(ProcessServerManager.class);
    private static final Logger oupLogger = MessageHandler.getInstance().getLogger(ProcessServerManager.class.getName() + "#output");
    
    private static final Set<String> attSet = Collections.unmodifiableSet(
        ConversionUtil.getInstance().toSet(
                new String[] {RMI_REGISTRY_PORT, CALLBACK_OBJECT_PATH}));

    private final ConcurrentHashMap <InetAddress, DelayedResult<ProcServer>> activeTable = 
        new ConcurrentHashMap<InetAddress, DelayedResult<ProcServer>>();
    
    private final ConcurrentHashMap<Integer, List<IProcessEventListener>> listeners =
    		new ConcurrentHashMap<Integer, List<IProcessEventListener>>();
    
    private final Map <Long, DelayedResult<IProcessServer>> pendingCallbacks = 
    			Collections.synchronizedMap(
    						new HashMap<Long, DelayedResult<IProcessServer>>());
    
    private final Map <String, String> attributes = 
    			new HashMap<String, String>();
    
    private final AtomicLong ids = new AtomicLong(0);
    
    private final AtomicInteger startState = new AtomicInteger(STOPPED);
    
    private final RMICallbackObject callback =
    		new RMICallbackObject();
    
    private Registry registry;

    public ProcessServerManager(){ }
    
    /* (non-Javadoc)
     * @see ddproto1.launcher.procserver.IProcessServerManager#getProcessServerFor(ddproto1.configurator.commons.IConfigurable, ddproto1.launcher.procserver.IRemoteCommandExecutor)
     */
    public IProcessServer getProcessServerFor(IConfigurable configurable,
    		IRemoteCommandExecutor executor)
    		throws AttributeAccessException, UnknownHostException, RemoteException,
    			InterruptedException, ExecutionException, LauncherException
    {
        if (startState.get() != STARTED)
            throw new IllegalStateException("Service not yet started.");

        /** Starts by constructing an address-port pair for this server. */
        String address = configurable.getAttribute(LOCAL_AGENT_ADDRESS);

        InetAddress inetAddr = InetAddress.getByName(address);

        long startTime = System.currentTimeMillis();

        while (true) {
            DelayedResult<ProcServer> psft = activeTable.get(inetAddr);

            if (psft == null) {
                DelayedResult<ProcServer> tmp = new DelayedResult<ProcServer>();
                psft = activeTable.putIfAbsent(inetAddr, tmp);

                if (psft == null) {
                    /**
                     * This thread has been ellected to perform the launch. All
                     * other threads will block on get just below.
                     */
                    try {
                        ProcServer ps = launch(executor);
                        tmp.set(ps);
                        return ps.getServer();
                    } catch (Exception ex) {
                        tmp.setException(ex);
                        throw new ExecutionException(ex.getCause());
                    }
                }
            }

            ProcServer remoteServer = null;
            try {
                remoteServer = psft.get();
                if (remoteServer.getServer().isAlive()) {
                    return remoteServer.getServer();
                }
            } catch (Throwable ex) {
                /**
                 * Something went bad. Either the server died or something else
                 * went wrong.
                 * 
                 * If we timeouted without being able to acquire a viable
                 * reference, abort with an error.
                 */
                if (System.currentTimeMillis() - startTime >= CALLBACK_TIMEOUT)
                    throw new LauncherException("Could not acquire reference "
                            + "to remote server under the required timeframe.");

                if (!meansDeadServer(ex) && !isDead(remoteServer.getSpawnSSH())) {
                    /**
                     * We could not establish that the error is because the
                     * remote server is dead. Throw the exception up.
                     */
                    throw new LauncherException("Can't decide on status of remote server.", ex);
                }

                /**
                 * Removes the future for the dead server if someone else
                 * haven't already.
                 */
                activeTable.remove(inetAddr, psft);
            }
        }
    }
        
    private ProcServer launch(IRemoteCommandExecutor executor)
    		throws AttributeAccessException, LauncherException, InterruptedException
    {
    	long id = ids.getAndIncrement();
        try{
            executor.getCommandLine().setAttribute(IConfigurationConstants.ID_ATTRIB, Long.toString(id));
        }catch(ConfigException ex){
            throw new LauncherException("Cannot launch process server - configuration error.", ex);
        }
        				
		DelayedResult <IProcessServer> dl = new DelayedResult<IProcessServer>();
		
        /** Register a future task for this id. */  
        if(startState.get() != STARTED)
            throw new InterruptedException();
        pendingCallbacks.put(id, dl);
        
		Process proc = null;
		try{
			proc = executor.executeRemote();
		}catch(Throwable ex){
			if(proc != null) proc.destroy();
			throw new LauncherException("Error while launching server.", ex);
		}
		
		try{
			IProcessServer pServer = dl.get(CALLBACK_TIMEOUT, TimeUnit.MILLISECONDS);
			return new ProcServer(proc, pServer, id);
		}catch(ExecutionException ex){
			logger.error("Error while launching server.", ex.getCause());
			removeAndKill(proc, id);			
			throw new LauncherException(ex.getCause());
		}catch(TimeoutException ex){
			if(isDead(proc))
				logger.error("Remote server has prematurely died.");
			else
				logger.error("Remote server has timeouted.");
			
            logger.error("Attempting to dump buffered content of streams server.");
			
			dumpStreamNonblock("Error stream", proc.getErrorStream(), logger);
			dumpStreamNonblock("Standard output", proc.getInputStream(), logger);
			
			removeAndKill(proc, id);
			throw new LauncherException(ex);
		}catch(InterruptedException ex){
			logger.error("Launch thread interrupted.");
			removeAndKill(proc, id);
			throw new LauncherException(ex);
		}
    }
    
    private void dumpStreamNonblock(String label, InputStream is, Logger logger){
    		logger.error("******* Dumping stream contents of " + label + " ********* ");
    		
    		try{
    		    int willDump = is.available();
                if(is.available() > 0){
                    byte [] buffer = new byte[willDump];
                    is.read(buffer);
                    logger.error(new String(buffer));
                }else{
                    logger.error("Nothing to dump.");
                }
    		}catch(IOException ex){
    			logger.error("Error while dumping stream - IOException.");
    		}
    }
    
    private void removeAndKill(Process proc, long id){
		pendingCallbacks.remove(id);
		if(!isDead(proc)) proc.destroy();
    }
    
    private boolean isDead(Process proc){
    		try{
    			proc.exitValue();
    		}catch(IllegalThreadStateException ex){
    			return false;
    		}
    		
    		return true;
    }
    
    private boolean meansDeadServer(Throwable ex){
    		return (ex instanceof ConnectException) ||
    			(ex instanceof ConnectIOException) ||
                (ex.getCause() instanceof EOFException);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.launcher.procserver.IProcessServerManager#registerProcessListener(ddproto1.launcher.procserver.IProcessEventListener, int)
     */
    public synchronized void registerProcessListener(IProcessEventListener listener, int pHandle){
    		List<IProcessEventListener> listenerList;
    		if(listeners.containsKey(pHandle)){
    			listenerList = listeners.get(pHandle);
    		}else{
    			listenerList = new CopyOnWriteArrayList<IProcessEventListener>();
    			listeners.put(pHandle, listenerList);
    		}
    		
    		listenerList.add(listener);
    }
    /* (non-Javadoc)
     * @see ddproto1.launcher.procserver.IProcessServerManager#removeProcessListener(ddproto1.launcher.procserver.IProcessEventListener, int)
     */
    public synchronized boolean removeProcessListener(IProcessEventListener listener,
			int pHandle) {
		List<IProcessEventListener> listenerList;
		if (!listeners.containsKey(pHandle))
			return false;

		listenerList = listeners.get(pHandle);
		boolean removed = listenerList.remove(listener);
		if (listenerList.isEmpty())
			listeners.remove(pHandle);
		return removed;
	}
    
    private List<IProcessEventListener> listenersForHandle(int procHandle){
    		/** Since we use ConcurrentHashMap, reads can be 
    		 * unsynchronized. 
    		 */
    		List <IProcessEventListener> list = listeners.get(procHandle);
    		return (list == null)?EMPTY_LIST:list;
    }
    
    public boolean startIfPossible()
        throws Exception
    {
        if(!startState.compareAndSet(STOPPED, STARTING))
            return false;
        else{ 
            startInternal(true);
            return true;
        }
    }
    
    public void start() throws Exception{
        this.startInternal(false);
    }
    
    private void startInternal(boolean bypassCheck) 
    		throws Exception
    	{
    		if(!bypassCheck && !startState.compareAndSet(STOPPED, STARTING))
    			throw new IllegalStateException("You can't start a non-stopped service.");
    		
    		try {
        		int registryPort = 
        			Integer.parseInt(getAttribute(RMI_REGISTRY_PORT));
        		String objectPath = 
        			getAttribute(CALLBACK_OBJECT_PATH);

			PortableRemoteObject.exportObject(callback);
            startRMIRegistry(registryPort);
			getRMIRegistry().rebind(objectPath, 
						PortableRemoteObject.toStub(callback));
			
			startState.set(STARTED);
		} catch (Exception ex) {
			try{
				PortableRemoteObject.unexportObject(callback);
			}catch(NoSuchObjectException exx) {  
                /** It's okay to swallow this. */
            }catch(Exception exx){
                /** Failed to unexport but we don't know why. The object
                 * might still be exported, in which case subsequent attempts
                 * to export it will fail. This should be rare, so I won't
                 * attempt to recover or track it. */
                logger.error("Failed to unexport RMI object. Server might " +
                        "not be startable anymore.");
            }
            /** If the RMI registry has been started, we must
             * stop it.
             */
            if(getRMIRegistry() != null)
                stopRMIRegistry();
            
			startState.set(STOPPED);
			throw ex;
		}
	}
    
	public void stop() throws Exception {
	    if(!startState.compareAndSet(STARTED, STOPPING))
	        throw new IllegalStateException("You cannot stop a non-started service.");

        shutdownAllServers();
		getRMIRegistry().unbind(getAttribute(CALLBACK_OBJECT_PATH));
		PortableRemoteObject.unexportObject(callback);
        stopRMIRegistry();
		startState.set(STOPPED);
	}
    
    private void startRMIRegistry(int registryPort)
        throws RemoteException
    { 
        assert getRMIRegistry() == null;
        setRMIRegistry(LocateRegistry.createRegistry(registryPort));
    }
    
    private void shutdownAllServers()
        throws InterruptedException
    {
        /** This loop will see all ProcServer instances created 
         * until before the state changed to STOPPING. We ensure
         * that futures that are not yet in table can't result 
         * in launches by checking for cancellation after adding the
         * future to the table but before performing the actual
         * launch.
         *  
         * For each future in table, we have the following 
         * possibilities:
         * 
         * 1) Future is done
         *   1.1) And process server is alive. We kill it.
         *   1.2) And process server is dead. We do nothing.
         *   1.3) And we don't know if process server is dead. 
         *        We try to kill it and log it if we fail.
         * 
         * 2) Future isn't done. We wait for a while (CALLBACK_TIMEOUT 
         * milliseconds) until it's done and then try to kill the server. 
         * If we can't kill the server, we log the error. It might 
         * be that we leave servers alive, but that chance is equal to 
         * the chance that we fail to see that a server has came up in 
         * the first place.
         *     
         */
        for(InetAddress address : activeTable.keySet()){
            DelayedResult<ProcServer> pServerFuture = activeTable.get(address);
            ProcServer instance = null;
            
            try{
                instance = pServerFuture.get(CALLBACK_TIMEOUT, TimeUnit.MILLISECONDS);
            }catch(ExecutionException ex){
                // OK. Launch has failed. This should have already 
                // been handled by the launcher thread. This future is done.
            }catch(TimeoutException ex){
                // Future wasn't done and timeouted. Let it be.
                logger.error("Request timeouted while attempting to acquire reference to " +
                        "process server at " + address + ". You will have to shut it down" +
                                " manually if it isn't dead already.");
            }catch(InterruptedException ex){
                throw ex;
            }catch(Throwable ex){
                // Unexpected exception.
                logger.warn("Unexpected exception thrown while shutting down process server. " +
                        "Process server at " + address + " won't be shut down.", ex);
            }
            
            if(instance == null) continue;
            
            IProcessServer pServer = instance.getServer();
            Process pyExpect = instance.getSpawnSSH();
            
            tryKill(pServer, address);
            tryKill(pyExpect, address);
        }
    }
    
    private void tryKill(IProcessServer pServer, InetAddress addr){
        try{
            pServer.isAlive();
        }catch(Throwable t){
            if(meansDeadServer(t)) return;
        }
        
        try{
            pServer.shutdownServer(true, DebuggerConstants.DEFAULT_PROCSERVER_SHUTDOWN_TIMEOUT);
            return;
        }catch(Throwable t){
            if(meansDeadServer(t)) return;
        }
        
        logger.error("Failed to shut down process server at " + addr);
    }
    
    private void tryKill(Process proc, InetAddress addr) throws InterruptedException{
        long startTime = System.currentTimeMillis();
        
        proc.destroy();
        
        while((System.currentTimeMillis() - startTime) <= CALLBACK_TIMEOUT){
            try{
                proc.exitValue();
                return;
            }catch(IllegalThreadStateException ex){
                Thread.sleep(DEATH_POLL);
                continue;
            }
        }
        
        logger.error("Unable to shut down remote process executor for address " 
                + addr + " in reasonable ammount of time. It'll be left running.");
    }
    
    private void stopRMIRegistry(){
        Registry reg = getRMIRegistry();
        assert reg != null;
        try{
            UnicastRemoteObject.unexportObject(reg, true);
        }catch(NoSuchObjectException ex){
            logger.error("RMI registry had been previously unexported.", ex);
        }
        setRMIRegistry(null);
    }
    	
	private synchronized void setRMIRegistry(Registry reg){
		this.registry = reg;
	}
	
	private synchronized Registry getRMIRegistry(){
		return registry;
	}

	public boolean isWritable() {
		return true;
	}

	public synchronized String getAttribute(String key) 
		throws IllegalAttributeException, UninitializedAttributeException {
		return attributes.get(key);
	}

	public synchronized void setAttribute(String key, String val) 
		throws IllegalAttributeException, InvalidAttributeValueException {
		attributes.put(key, val);
	}

    private class ProcServer {
		private volatile Process spawnSSH;
		private volatile IProcessServer server;
		private volatile long id;
	
		public ProcServer(Process spawnSSH, IProcessServer server,
				long id) {
			this.spawnSSH = spawnSSH;
			this.server = server;
			this.id = id;
		}
	
		public IProcessServer getServer() {
			return server;
		}
	
		public Process getSpawnSSH() {
			return spawnSSH;
		}
        
        public long getId(){
            return id;
        }
	}

	private class RMICallbackObject implements IControlClient {
	
		public void notifyProcessDeath(int pHandle, int exitValue) throws RemoteException {
            if(oupLogger.isDebugEnabled())
                oupLogger.debug("Process [" + pHandle + "] has died.");
			for (IProcessEventListener client : listenersForHandle(pHandle)) {
				client.notifyProcessKilled(exitValue);
			}
		}
	
		public void receiveStringFromSTDOUT(int pHandle, String data)
				throws RemoteException {		    
            if(oupLogger.isDebugEnabled())
                oupLogger.debug("Process [" + pHandle + ", stdout]: " + data);
            
			for (IProcessEventListener client : listenersForHandle(pHandle)) {
				client.notifyNewSTDOUTContent(data);
			}
		}
	
		public void receiveStringFromSTDERR(int pHandle, String data)
				throws RemoteException {
            if(oupLogger.isDebugEnabled())
                oupLogger.debug("Process [" + pHandle + ", stderr]: " + data);

			for (IProcessEventListener client : listenersForHandle(pHandle)) {
				client.notifyNewSTDERRContent(data);
			}
		}
	
		public void notifyServerUp(IProcessServer procServer)
				throws RemoteException {
			
			Long id = null;
			try{
				String cookie = procServer.getCookie();
				id = Long.parseLong(cookie);
			}catch(Throwable t){
				logger.error("Could not bind process server to ID. Launcher thread will timeout.", t);
                return;
			}

            DelayedResult<IProcessServer> 
                pendingCallback = pendingCallbacks.remove(id);
        
            // If this happens, hell will break loose.
            assert pendingCallback != null;
        

            pendingCallback.set(procServer);
		}
	}

    public Set<String> getAttributeKeys() {
        return attSet;
    }

    public int currentState() {
        return startState.get();
    }
}
