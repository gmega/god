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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.rmi.PortableRemoteObject;

import ddproto1.controller.constants.IErrorCodes;
import ddproto1.controller.exception.ServerRequestException;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemotable;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;

/**
 * This class is thread-safe (or at least it should be).
 * 
 * @author giuliano
 *
 */
public class RemoteProcessServerImpl implements IErrorCodes, IProcessServer, IRemotable {

    private static final int POLLING_THREADS = 10;
    private static final int SHUTDOWN_BACKOFF = 2000;
    
    private final List<RemoteProcessImpl> processList = new LinkedList<RemoteProcessImpl>();
    private final AtomicReference<String> cookie = new AtomicReference<String>();
    private final ScheduledExecutorService poller = new ScheduledThreadPoolExecutor(POLLING_THREADS);
    private IControlClient client;
    
    private Remote proxy;

    public RemoteProcessServerImpl(IControlClient client){
        setControlClient(client);
    }
        
    
    public IRemoteProcess launch(LaunchParametersDTO parameters)
            throws ServerRequestException {
        
        if(parameters.getPollInterval() <= 0) 
            throw new ServerRequestException("Polling interval must be a non-negative integer.");
        
        Process launched = null;
        
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
        
        ProcessPollTask ppt = new ProcessPollTask(getControlClient(), proc, handle);
        ppt.scheduleOnExecutor(poller, pollInterval, TimeUnit.MILLISECONDS);

        rpi.beginDispatchingStreams();
        
        synchronized(processList){
            processList.add(rpi);
        }

        return rpi;
    }
    
    protected void disposeCalled(RemoteProcessImpl rImpl){
        synchronized(processList){
            boolean removed = processList.remove(rImpl);
            assert removed;
        }
    }

    public synchronized void dispose() 
        throws NoSuchObjectException
    {
        PortableRemoteObject.unexportObject(this);
    }

    public synchronized void shutdownServer(boolean shutdownChildProcesses) 
        throws RemoteException
    {
        
        /** Terminates all children processes. */
        if(shutdownChildProcesses){
            List plistCopy = null;
        
            synchronized(processList){
                plistCopy = 
                    new LinkedList(processList);
            }
        
            for(RemoteProcessImpl rpi : (LinkedList<RemoteProcessImpl>)plistCopy){
                rpi.dispose();
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
