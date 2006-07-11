/*
 * Created on Jun 19, 2006
 * 
 * file: AbstractControlClient.java
 */
package ddproto1.controller.remote.test;

import junit.framework.TestCase;

public class ControlClientOps {
    protected class ProcessState{
        protected volatile int handle;
        protected volatile boolean stdoutPrinted = false;
        protected volatile boolean stderrPrinted = false;
        protected volatile boolean dead = false;
    }
    
    private volatile boolean failed = false;
    
    protected ProcessState processes[];
    private volatile int pLength;
    
    public ControlClientOps(int nProcs){
        pLength = nProcs;
        synchronized(this){
            processes = new ProcessState[nProcs];
            
            for(int i = 0; i < nProcs; i++){
                processes[i] = new ProcessState();
                processes[i].handle = i;
            }
        }
    }
    
    protected synchronized ProcessState getProcess(int i){ return processes[i]; }
    
    protected boolean checkHandle(int handle){
        if(handle > pLength){
            System.err.println("Caught invalid process handle: " + handle);
            failed = true;
            return false;
        }
        
        return true;
    }
    
    public void notifyProcessDeath(int pHandle) {
        if(!checkHandle(pHandle)) return;
        System.out.println("Got process death event.");
        if(getProcess(pHandle).dead){
            System.err.println("Double death detected for " + pHandle);
            failed = true;
        }
        getProcess(pHandle).dead = true;
    }

    public void receiveStringFromSTDOUT(int pHandle, String data) {
        if(!checkHandle(pHandle)) return;
        if(!data.equals("Stdout: " + pHandle)){
            System.err.println("Wrong stdout value detected for " + pHandle);
            System.err.println("Got: " + data);
            failed = true;
        }
            
        getProcess(pHandle).stdoutPrinted = true;
    }
    
    public void receiveStringFromSTDERR(int pHandle, String data) {
        if(!checkHandle(pHandle)) return;
        if(!data.equals("Stderr: " + pHandle)){
            System.err.println("Wrong stderr value detected for " + pHandle);
            System.err.println("Got: " + data);
            failed = true;
        }
        getProcess(pHandle).stderrPrinted = true;
    }
    
    public boolean isDone(){
        TestCase.assertFalse(failed);
        boolean done = true;
        
        for(ProcessState process : processes){
            done &= (process.dead & process.stdoutPrinted & process.stderrPrinted);
        }
        
        return done;
    }
}
