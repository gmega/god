/*
 * Created on Jan 31, 2005
 * 
 * file: JDIMiscTrait.java
 */
package ddproto1.util.traits;

import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

/**
 * @author giuliano
 *
 */
public class JDIMiscTrait {
    private static JDIMiscTrait instance = null;

    public synchronized static JDIMiscTrait getInstance(){
        return (instance == null)?(instance = new JDIMiscTrait()):(instance);
    }
    
    private JDIMiscTrait(){ }
    
    public void clearPreviousStepRequests(ThreadReference thread){
        EventRequestManager mgr = thread.virtualMachine().eventRequestManager();
        List requests = mgr.stepRequests();
        Iterator iter = requests.iterator();
        while (iter.hasNext()) {
            StepRequest request = (StepRequest) iter.next();
            if (request.thread().equals(thread)) {
                mgr.deleteEventRequest(request);
                break;
            }
        }
    }
}
