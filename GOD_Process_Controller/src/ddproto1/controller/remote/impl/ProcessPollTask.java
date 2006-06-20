/*
 * Created on Jun 17, 2006
 * 
 * file: ProcessPollTask.java
 */
package ddproto1.controller.remote.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import Ice.LocalException;
import ddproto1.controller.client.AMI_ControlClient_notifyProcessDeath;
import ddproto1.controller.client.ControlClientPrx;

public class ProcessPollTask implements Runnable{
    
    private static final Logger logger = Logger.getLogger(ProcessPollTask.class);
    
    private Process toPoll;
    private ControlClientPrx controlClient;
    private volatile int handle;
    private final AtomicBoolean notifying = new AtomicBoolean(false);
    
    private ScheduledFuture sf;
        
    public ProcessPollTask(ControlClientPrx cc, Process toPoll, int handle){
        setProcess(toPoll);
        setControlClient(cc);
        this.handle = handle;
    }
    
    private synchronized ControlClientPrx getControlClient() { return controlClient; }
    private synchronized void setControlClient(ControlClientPrx controlClient) {
        this.controlClient = controlClient;
    }
    private synchronized void setProcess(Process toPoll){ this.toPoll = toPoll; }
    private synchronized Process getProcess() { return this.toPoll; }
    
    public synchronized void scheduleOnExecutor(ScheduledExecutorService service, long periodicity, TimeUnit tu){
        sf = service.scheduleAtFixedRate(this, 0, periodicity, tu);
    }
    
    private synchronized ScheduledFuture getExecutorHandle(){
        return sf;
    }

    public void run() {
        try{
            getProcess().exitValue();
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
         * on failure in the future ( 
         */
        if(!notifying.compareAndSet(false, true)) return;
        
        getControlClient().notifyProcessDeath_async(
                    new AMI_ControlClient_notifyProcessDeath(){
                        @Override
                        public void ice_response() { }

                        @Override
                        public void ice_exception(LocalException ex) {
                            logger.error("Process death notification failed.", ex);
                        }
                    }, handle);    
        
        /* Cancels the task after sending update. Note that we are not very 
         * robust in the sense that we don't handle errors. Ideally, we 
         * should retry delivery for a while if it fails, but currently we just fail 
         * with an error message. I don't think this is bad because I'm trusting
         * that ICE and TCP will do everything they can to deliver the message 
         * before failing. Anyway, setting 'notifying' to false on error will
         * force retry. Of course we would then have to cancel the task on ice_response.
         * That leads me to think whether I should have used async notifications here
         * or not. :-P */
        ourHandle.cancel(false);
        notifying.set(false);
    }
   
}