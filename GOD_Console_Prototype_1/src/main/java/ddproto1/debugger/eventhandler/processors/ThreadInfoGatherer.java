/*
 * Created on Feb 3, 2005
 * 
 * file: ThreadInfoGatherer.java
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.GlobalAgent;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IJavaThreadManager;
import ddproto1.debugger.managing.IThreadManager;
import ddproto1.debugger.managing.ThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * @author giuliano
 *
 */
public class ThreadInfoGatherer extends AbstractEventProcessor {

    private static final MessageHandler mh = MessageHandler.getInstance();
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    public ThreadInfoGatherer(){  }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.BasicEventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    public void specializedProcess(Event e) {
        try{
            ThreadReference tr = ThreadManager.getThreadFromEvent(e);
            String vmid = (String)e.request().getProperty(DebuggerConstants.VMM_KEY);
            IJavaNodeManager vmm = 
                (IJavaNodeManager)VMManagerFactory.getInstance().getNodeManager(vmid).getAdapter(IJavaNodeManager.class);
            IJavaThreadManager vmt = vmm.getThreadManager();
            
            Integer ltid = vmt.getThreadUUID(tr);
            ILocalThread ltr = vmt.getLocalThread(ltid);
            
            String hexid = ct.long2Hex(tr.uniqueID());
            String str_ltid = null;
            String str_dtid = null;
            
            if(ltid != null){
                str_ltid = ct.uuid2Dotted(ltid.intValue());
                IDistributedThread dt = ltr.getParentDistributedThread();
                Integer dt_uuid = null;
                if(!(dt instanceof NilDistributedThread)) dt_uuid = dt.getId();
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
