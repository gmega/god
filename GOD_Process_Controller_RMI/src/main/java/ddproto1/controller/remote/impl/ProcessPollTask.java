/*
 * Created on Jun 17, 2006
 * 
 * file: ProcessPollTask.java
 */
package ddproto1.controller.remote.impl;

import java.rmi.RemoteException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import ddproto1.controller.interfaces.IControlClient;

public class ProcessPollTask implements Runnable{
    
    private static final Logger logger = Logger.getLogger(ProcessPollTask.class);
    
    private Process toPoll;
    private IControlClient controlClient;
    private volatile int handle;
    private final AtomicBoolean notifying = new AtomicBoolean(false);
    
    private ScheduledFuture sf;
        
    public ProcessPollTask(IControlClient cc, Process toPoll, int handle){
        setProcess(toPoll);
        setControlClient(cc);
        this.handle = handle;
    }
    
    private synchronized IControlClient getControlClient() { return controlClient; }
    private synchronized void setControlClient(IControlClient controlClient) {
        this.controlClient = controlClient;
    }
    private synchronized void setProcess(Process toPoll){ this.toPoll = toPoll; }
    private synchronized Process getProcess() { return this.toPoll; }
    
    public synchronized void scheduleOnExecutor(ScheduledExecutorService service, long periodicity, TimeUnit tu){
        sf = service.scheduleAtFixedRate(this, 0, periodicity, tu);
    }
    
    public boolean isDone(){
        ScheduledFuture sf = getExecutorHandle();
        if (sf == null) return false;
        return sf.isDone();
    }
    
    private synchronized ScheduledFuture getExecutorHandle(){
        return sf;
    }
    
    public void run() {
        
        int exitValue;
        
        try{
            exitValue = getProcess().exitValue();
        }catch(IllegalThreadStateException ex){ 
            return;
        }
        
        // I'm trusting that ScheduledFuture is thread-safe. 
        ScheduledFuture ourHandle = this.getExecutorHandle();
        if(ourHandle.isDone()) return;
        
        /* This will avoid duplicate notifications, so its okay
         * if the task wasn't marked as done just above. The task
         * will just keep (uselessly) on being scheduled until it 
         * completes, but it won't behave erratically. I won't cancel
         * the task here because I might want to keep on scheduling it
         * on failure in the future. 
         */
        logger.debug("Acquiring lock to notification.");
        if(!notifying.compareAndSet(false, true)) return;
        
        try{
            getControlClient().notifyProcessDeath(handle, exitValue);
        }catch(RemoteException ex){
            logger.error("Failed to notify control client of process death (handle: " 
                    + handle + ")");
            //notifying.set(false); return; This will allow retries.
        }
        
        logger.debug("Process poll task has performed its notification.");
        
        /* Cancels the task after sending update. Note that we are not very 
         * robust in the sense that we don't handle errors. Ideally, we 
         * should retry delivery for a while if it fails, but currently we just fail 
         * with an error message. */
        ourHandle.cancel(false);
    }
   
}