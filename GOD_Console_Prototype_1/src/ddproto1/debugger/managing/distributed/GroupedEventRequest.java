/*
 * Created on 31/01/2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ddproto1.debugger.managing.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;

import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GroupedEventRequest implements EventRequest, IGroupedRequest{

    private static final MessageHandler mh = MessageHandler.getInstance();
    
    private Set resolved;
    private Map unresolved;
    private boolean enabled = false;
    private int suspendPolicy = -1;
    
    public GroupedEventRequest(Map reqlist){
        resolved = new HashSet();
        unresolved = new HashMap(reqlist);
    }
    
	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#isEnabled()
	 */
	public boolean isEnabled() {
	    return enabled;
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#setEnabled(boolean)
	 */
	public synchronized void setEnabled(boolean val) {
	    enabled = val;
	    if(val == true){
	        enableAllUnresolved();
	        enableAllResolved();
	    }else{
	        disableAllUnresolved();
	        disableAllResolved();
	    }
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#enable()
	 */
	public synchronized void enable() {
	    if(enabled == false){
	        enableAllUnresolved();
	        enableAllResolved();
	    }
	    enabled = true;
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#disable()
	 */
	public synchronized void disable() {
	    if(enabled == true){
	        disableAllResolved();
	        disableAllUnresolved();
	    }
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#addCountFilter(int)
	 */
	public synchronized void addCountFilter(int arg0) {
		Iterator it = resolved.iterator();
		while(it.hasNext()){
		    try{
		        EventRequest er = (EventRequest)it.next();
		        er.addCountFilter(arg0);
		    }catch(ClassCastException e){
		        mh.getErrorOutput().println("Error - a resolved request " +
		        		"doesn't match the required type for grouping with" +
		        		" EventRequests.");
		    }catch(Exception e){
		        mh.printStackTrace(e);
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#setSuspendPolicy(int)
	 */
	public void setSuspendPolicy(int arg0) {
		Iterator it = resolved.iterator();
		while(it.hasNext()){
		    try{
		        EventRequest er = (EventRequest)it.next();
		        er.setSuspendPolicy(arg0);
		    }catch(ClassCastException e){
		        mh.getErrorOutput().println("Error - a resolved request " +
		        		"doesn't match the required type for grouping with" +
		        		" EventRequests.");
		    }catch(Exception e){
		        mh.printStackTrace(e);
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#suspendPolicy()
	 */
	public int suspendPolicy() {
	    if(suspendPolicy == -1)
	        throw new IllegalStateException("Group suspend policy is yet undefined.");
		return suspendPolicy;
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#putProperty(java.lang.Object, java.lang.Object)
	 */
	public void putProperty(Object arg0, Object arg1) {
		throw new UnsupportedException();		
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.request.EventRequest#getProperty(java.lang.Object)
	 */
	public Object getProperty(Object arg0) {
		throw new UnsupportedException();
	}

	/* (non-Javadoc)
	 * @see com.sun.jdi.Mirror#virtualMachine()
	 */
	public VirtualMachine virtualMachine() {
	    throw new UnsupportedException ("Group requests are not " +
	    		"associated to virtual machines.");
	}

    /* (non-Javadoc)
     * @see ddproto1.debugger.managing.distributed.IGroupedRequest#queryInterface(java.lang.Class)
     */
    public Object queryInterface(Class intf) {
        if(EventRequest.class.isAssignableFrom(intf))
            return this;
        else return null;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IResolutionListener#notifyResolution(ddproto1.debugger.request.IDeferrableRequest, java.lang.Object)
     */
    public synchronized void notifyResolution(IDeferrableRequest source, Object byproduct) {
        // TODO Auto-generated method stub
        
    }
    
    private void disableAllResolved(){
        
    }
    
    private void enableAllResolved(){
        
    }
    
    private void enableAllUnresolved(){
        
    }
    
    private void disableAllUnresolved(){
        
    }
}
