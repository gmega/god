/*
 * Created on Jun 16, 2006
 * 
 * file: ProcessServerImpl.java
 */
package ddproto1.controller.remote.impl;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Logger;

import ddproto1.controller.constants.IErrorCodes;
import ddproto1.controller.exception.ServerRequestException;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import ddproto1.controller.interfaces.internal.IRemotable;

/**
 * This class is thread-safe (or at least it should be).
 * 
 * @author giuliano
 *
 */
public class RemoteProcessServerImpl implements IErrorCodes, IProcessServer, IRemotable {

    private static final int POLLING_THREADS = 10;
    private static final int SHUTDOWN_BACKOFF = 2000;
    private static final int SHUTDOWN_COUNTER_POLL = 100;
    
    private static final Logger logger = Logger.getLogger(RemoteProcessImpl.class);
    
    /** List of non-disposed process proxies. */
    private final List<RemoteProcessImpl> processList = new LinkedList<RemoteProcessImpl>();
    
    /** Flag that tells whether the process server is shutting down or not. */
    private final AtomicBoolean fShuttingDown = new AtomicBoolean(false);
    
    /** Counter of live processes. */
    private final AtomicInteger fActiveTasks = new AtomicInteger(0);
    
    /** Storage reference for the client-side cookie. */
    private final AtomicReference<String> cookie = new AtomicReference<String>();
    
    /** Executor for process polling tasks. */
    private final ScheduledExecutorService poller = new ScheduledThreadPoolExecutor(POLLING_THREADS);
    
    /** Reference to the remote client. */
    private IControlClient client;
    
    /** Reference to our own proxy. */
    private Remote proxy;

    public RemoteProcessServerImpl(IControlClient client){
        setControlClient(client);
    }
    
    public IRemoteProcess launch(LaunchParametersDTO parameters)
            throws ServerRequestException {
        
        if(parameters.getPollInterval() <= 0) 
            throw new ServerRequestException("Polling interval must be a non-negative integer.");
        
        Process launched = null;
        if(logger.isDebugEnabled())
            logger.debug("Launch requested for: \n" + parameters);
        
        try{
            launched = Runtime.getRuntime().exec(parameters.getCommandLine());
        }catch(IOException ex){
            throw new ServerRequestException("Launch failed." + ex.getMessage(), ex);
        }
        
        return processAdded(launched, parameters.getPollInterval(), parameters.getMaxUnflushedSize(), 
                parameters.getFlushTimeout(), parameters.getNumericHandle());
    }

    public LinkedList <IRemoteProcess> getProcessList() {
        synchronized(processList){
            return new LinkedList<IRemoteProcess>(processList);
        }
    }

    public boolean isAlive() {
        return true;
    }
    
    private synchronized void setControlClient(IControlClient client){
        this.client = client;
    }
    
    private synchronized IControlClient getControlClient(){
        return this.client;
    }
    
    private IRemoteProcess processAdded(Process proc, int pollInterval, 
    		int maxBufferSize, int flushTimeout, int handle)
        throws ServerRequestException
    {
        RemoteProcessImpl rpi = 
            new RemoteProcessImpl(proc, 
                    maxBufferSize, 
                    flushTimeout, 
                    handle,
                    getControlClient(),
                    this);

        try{ 
            rpi.getProxyAndActivate();
        }catch(RemoteException ex){
            throw new ServerRequestException("Error while attempting to " +
                    "export remote process proxy.", ex);
        }
        
        ProcessPollTask ppt = new ProcessPollTask(this, getControlClient(), proc, handle);
        
        ppt.scheduleOnExecutor(poller, pollInterval, TimeUnit.MILLISECONDS);
        fActiveTasks.incrementAndGet();

        rpi.beginDispatchingStreams();
        
        synchronized(processList){
            if(!fShuttingDown.get()){
                processList.add(rpi);
                return rpi;
            }
        }
        
        // Server is shutting down. Kills process.
        rpi.dispose();
        throw new ServerRequestException("Server is shutting down.");
    }
    
    protected void notifyCompletion(ProcessPollTask ppTask){
        int copy = fActiveTasks.decrementAndGet();
        assert copy >= 0;
    }
    
    protected void disposeCalled(RemoteProcessImpl rImpl){
        synchronized(processList){
            processList.remove(rImpl);
        }
    }

    public synchronized void dispose() 
        throws NoSuchObjectException
    {
        PortableRemoteObject.unexportObject(this);
    }

    public synchronized void shutdownServer(boolean shutdownChildren, 
            long serverTimeout) 
        throws RemoteException
    {
        logger.info("Shut down signalled.");
        
        if(!fShuttingDown.compareAndSet(false, true))
            throw new ServerRequestException("Server already shutting down.");
        
        if(shutdownChildren){
            try{
                shutdownChildren(serverTimeout);
            }catch(ServerRequestException ex){
                fShuttingDown.set(false);
                throw ex;
            }
        }
        
        this.dispose();
        
        /** RMI sucks. I have to call System.exit to 
         * shut down the server. */
        new Thread(new Runnable(){
            public void run() {
                try{
                    Thread.sleep(SHUTDOWN_BACKOFF);
                }catch(InterruptedException ex) { }
                
                System.exit(0);
            }
        }).start();
        
    }
    
    private void shutdownChildren(long timeout)
        throws ServerRequestException
    {

        Runnable runnable = new Runnable(){
            public void run(){
                /** Terminates all children processes. */
                List<RemoteProcessImpl> plistCopy = null;

                synchronized(processList){
                    plistCopy = 
                        new LinkedList<RemoteProcessImpl>(processList);
                }
        
                logger.info("Terminating child processess...");
        
                for(RemoteProcessImpl rpi : (LinkedList<RemoteProcessImpl>)plistCopy){
                    rpi.dispose();
                }
            }
        };
        
        Thread shutdownThread = new Thread(runnable);
        shutdownThread.start();
        
        // Asynchronous shutdown.
        if(timeout < 0) return;
        
        long realTimeout;
        
        if(timeout == 0)
            realTimeout = Long.MAX_VALUE; // Waits for as long as it has to wait.
        else 
            realTimeout = timeout; // Timed shutdown.
        
        long initial = System.currentTimeMillis();
        
        try{
            logger.info("Now polling counter (active tasks:" + fActiveTasks.get() + ", timeout: " + realTimeout+ ")");
            while(fActiveTasks.get() != 0){
                Thread.sleep(SHUTDOWN_COUNTER_POLL);
                if(System.currentTimeMillis() - initial > realTimeout)
                    throw new ServerRequestException("Shutdown sequence timeouted.", null, IErrorCodes.TIMEOUT);
            }
            logger.info("Shutdown complete.");
        }catch(InterruptedException ex){
            throw new ServerRequestException("Shutdown sequence interrupted");
        }
            
    }

    public synchronized Remote getProxyAndActivate() 
        throws RemoteException, NoSuchObjectException
    {
        if(proxy == null){
            PortableRemoteObject.exportObject(this);
            proxy = PortableRemoteObject.toStub(this);
        }
        
        return proxy;
    }
    
    public String getCookie(){
    		return cookie.get();
    }
    
    public void setCookie(String cookie){
    		this.cookie.set(cookie);
    }
}
