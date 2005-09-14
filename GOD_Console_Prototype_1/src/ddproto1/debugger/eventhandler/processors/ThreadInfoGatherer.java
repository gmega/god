/*
 * Created on Feb 3, 2005
 * 
 * file: ThreadInfoGatherer.java
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;

import ddproto1.Main;
import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.ThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionTrait;

/**
 * @author giuliano
 *
 */
public class ThreadInfoGatherer extends BasicEventProcessor {

    private static final MessageHandler mh = MessageHandler.getInstance();
    private static final ConversionTrait ct = ConversionTrait.getInstance();
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.BasicEventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    protected void specializedProcess(Event e) {
        try{
            ThreadReference tr = ThreadManager.getThreadFromEvent(e);
            String vmid = (String)e.request().getProperty(DebuggerConstants.VMM_KEY);
            VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vmid);
            IVMThreadManager vmt = vmm.getThreadManager();
            
            Integer ltid = vmt.getThreadUUID(tr);
            
            String hexid = ct.long2Hex(tr.uniqueID());
            String str_ltid = null;
            String str_dtid = null;
            
            if(ltid != null){
                str_ltid = ct.uuid2Dotted(ltid.intValue());
                // This is horrible. There should be another way to get a DTM reference here.
                DistributedThreadManager dtm = Main.getDTM();
                Integer dt_uuid = dtm.getEnclosingDT(ltid); 
                if(dt_uuid != null) str_dtid = ct.uuid2Dotted(dt_uuid.intValue());
            }
                
            EventRequest req = e.request();
            req.putProperty("ltid", str_ltid);
            req.putProperty("dtid", str_dtid);
            req.putProperty("hexid", hexid);
            
        }catch(Exception ex){
            mh.getErrorOutput().println(
                    "Failed to gather thread information from event " + e
                            + ". Further errors may occur.");
            mh.printStackTrace(ex);
        }
    }

}
