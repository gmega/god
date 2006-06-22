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
    
    protected ProcessState processes[];
    
    public ControlClientOps(int nProcs){
        synchronized(this){
            processes = new ProcessState[nProcs];
            
            for(int i = 0; i < nProcs; i++){
                processes[i] = new ProcessState();
                processes[i].handle = i;
            }
        }
    }
    
    protected synchronized ProcessState getProcess(int i){ return processes[i]; }
    
    public void notifyProcessDeath(int pHandle) {
        System.out.println("Got process death event.");
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(!getProcess(pHandle).dead);
        getProcess(pHandle).dead = true;
    }

    public void receiveStringFromSTDOUT(int pHandle, String data) {
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(data.equals("Stdout: " + pHandle));
        getProcess(pHandle).stdoutPrinted = true;
    }

    public void receiveStringFromSTDERR(int pHandle, String data) {
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(data.equals("Stderr: " + pHandle));
        getProcess(pHandle).stderrPrinted = true;
    }
    
    public boolean isDone(){
        
        boolean done = true;
        
        for(ProcessState process : processes){
            done &= (process.dead & process.stdoutPrinted & process.stderrPrinted);
        }
        
        return done;
    }
}
