/*
 * Created on Jun 16, 2006
 * 
 * file: ProcessServerImpl.java
 */
package ddproto1.controller.remote.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Ice.Current;
import Ice.Identity;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;
import ddproto1.controller.client.ControlClientPrx;
import ddproto1.controller.client.LaunchParameters;
import ddproto1.controller.remote.RemoteProcessPrx;
import ddproto1.controller.remote.RemoteProcessPrxHelper;
import ddproto1.controller.remote.ServerRequestException;
import ddproto1.controller.remote._ProcessServerDisp;

public class ProcessServerImpl extends _ProcessServerDisp implements IErrorCodes, IRemoteAccessible {

    private static final int POLLING_THREADS = 10;
    
    private final List<RemoteProcessImpl> processList = new LinkedList<RemoteProcessImpl>();
    private final AtomicInteger handlerCounter = new AtomicInteger();
    private final ScheduledExecutorService poller = new ScheduledThreadPoolExecutor(POLLING_THREADS);
    private ControlClientPrx client;
    private ObjectAdapter adapter;
    private ObjectPrx proxy;
    private Identity identity;

    public ProcessServerImpl(ControlClientPrx client, ObjectAdapter objectAdapter){
        this(client, objectAdapter, null);
    }

    public ProcessServerImpl(ControlClientPrx client, ObjectAdapter objectAdapter, Identity identity){
        setControlClient(client);
        setAdapter(objectAdapter);
    }
    
    protected synchronized Identity getIdentity(){
        return identity;
    }
    
    protected synchronized void setIdentity(Identity identity){
        this.identity = identity;
    }

    
    protected synchronized ObjectAdapter getAdapter(){
        return adapter;
    }
    
    private synchronized void setAdapter(ObjectAdapter adapter){
        this.adapter = adapter;
    }
    
    public RemoteProcessPrx launch(LaunchParameters parameters, Current __current)
            throws ServerRequestException {
        
        if(parameters.pollInterval <= 0) 
            throw new ServerRequestException("Polling interval must be a non-negative integer.", INVALID_PARAMETER);
        
        Process launched = null;
        
        try{
            launched = Runtime.getRuntime().exec(parameters.commandLine);
        }catch(IOException ex){
            throw new ServerRequestException("Launch failed." + ex.getMessage(), IO_FAILURE);
        }
        
        return processAdded(launched, parameters.pollInterval, parameters.maxUnflushedSize, parameters.flushTimeout);
    }

    public LinkedList getProcessList(Current __current) {
        LinkedList pList = new LinkedList();
        synchronized(processList){
            for(RemoteProcessImpl rpimpl : processList)
                pList.add(RemoteProcessPrxHelper.uncheckedCast(rpimpl.activateAndGetProxy()));
        }
        return pList;
    }

    public boolean isAlive(Current __current) {
        return true;
    }
    
    private synchronized void setControlClient(ControlClientPrx client){
        this.client = client;
    }
    
    private synchronized ControlClientPrx getControlClient(){
        return this.client;
    }
    
    private RemoteProcessPrx processAdded(Process proc, int pollInterval, int maxBufferSize, int flushTimeout){
        
        int handle = handlerCounter.getAndAdd(1);
        
        RemoteProcessImpl rpi = 
            new RemoteProcessImpl(proc, 
                    maxBufferSize, 
                    flushTimeout, 
                    handle,
                    getControlClient(),
                    this);

        RemoteProcessPrx rpprx = 
            RemoteProcessPrxHelper.uncheckedCast(rpi.activateAndGetProxy());
        
        ProcessPollTask ppt = new ProcessPollTask(getControlClient(), proc, handle);
        ppt.scheduleOnExecutor(poller, pollInterval, TimeUnit.MILLISECONDS);
        
        rpi.beginDispatchingStreams();
        
        synchronized(processList){
            processList.add(rpi);
        }

        return rpprx;
    }
    
    protected void disposeCalled(RemoteProcessImpl rImpl){
        synchronized(processList){
            boolean removed = processList.remove(rImpl);
            assert removed;
        }
    }

    public synchronized ObjectPrx activateAndGetProxy() {
        if(proxy == null){
            if(identity == null){
                proxy = getAdapter().addWithUUID(this);
                setIdentity(proxy.ice_getIdentity());
            }else{
                proxy = getAdapter().add(this, identity);
            }
        }
        
        return proxy;
    }

    public synchronized void dispose() {
        getAdapter().remove(proxy.ice_getIdentity());
    }

    public synchronized void shutdownServer(boolean shutdownChildProcesses, Current __current) {
        
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
        
        /** Otherwise just shuts down the communicator. This will 
         * cause the server to exit, but launched processes won't 
         * be destroyed.*/
        adapter.getCommunicator().shutdown();
    }

}
