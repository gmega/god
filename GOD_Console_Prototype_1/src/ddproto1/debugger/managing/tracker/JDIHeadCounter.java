/*
 * Created on Sep 22, 2005
 * 
 * file: JDIHeadCounter.java
 */
package ddproto1.debugger.managing.tracker;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;

import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.IllegalStateException;
import ddproto1.util.traits.commons.ConversionTrait;

public class JDIHeadCounter implements IHeadCounter{

    private static ConversionTrait ct = ConversionTrait.getInstance(); 
    private static VMManagerFactory vmmf = VMManagerFactory.getInstance();

    public int headFrameCount(int uuid) throws IllegalStateException{
        return this.headFrameCount(uuid, false);
    }
    
    public int headFrameCount(int uuid, boolean suspend) throws IllegalStateException {
        byte node_uuid = ct.guidFromUUID(uuid);
        VirtualMachineManager vmm = vmmf.getVMManager(node_uuid);
        ThreadReference tr = vmm.getThreadManager().findThreadByUUID(uuid);
        
        boolean resume = false; 
        if(suspend){
            while(!tr.isSuspended()){
                tr.suspend();
                resume = true;
            }
        }
        
        if(!tr.isSuspended()) throw new IllegalStateException("Head thread not suspended.");
        try{
            int count = tr.frameCount();
            if(resume) while(tr.isSuspended()) tr.resume();
            return count;
        }catch(IncompatibleThreadStateException ex){
            throw new IllegalStateException(ex);
        }
    }
}
