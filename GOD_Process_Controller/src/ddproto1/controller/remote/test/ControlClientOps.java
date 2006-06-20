/*
 * Created on Jun 19, 2006
 * 
 * file: AbstractControlClient.java
 */
package ddproto1.controller.remote.test;

import junit.framework.TestCase;
import ddproto1.controller.client.AMI_ControlClient_notifyProcessDeath;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDERR;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDOUT;

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
    
    public void notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle) {
        System.out.println("Got process death event.");
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(!getProcess(pHandle).dead);
        getProcess(pHandle).dead = true;
        if(__cb != null) __cb.ice_response();
    }

    public void receiveStringFromSTDOUT_async(AMI_ControlClient_receiveStringFromSTDOUT __cb, int pHandle, String data) {
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(data.equals("Stdout: " + pHandle));
        getProcess(pHandle).stdoutPrinted = true;
        if(__cb != null)  __cb.ice_response();
    }

    public void receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data) {
        TestCase.assertTrue(getProcess(pHandle).handle == pHandle);
        TestCase.assertTrue(data.equals("Stderr: " + pHandle));
        getProcess(pHandle).stderrPrinted = true;
        if(__cb != null) __cb.ice_response();
    }
    
    public boolean isDone(){
        
        boolean done = true;
        
        for(ProcessState process : processes){
            done &= (process.dead & process.stdoutPrinted & process.stderrPrinted);
        }
        
        return done;
    }
}
