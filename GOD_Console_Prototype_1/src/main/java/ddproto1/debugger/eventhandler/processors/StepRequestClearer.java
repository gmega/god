package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.util.JDIMiscUtil;

public class StepRequestClearer extends AbstractEventProcessor{

	private JDIMiscUtil jmt = JDIMiscUtil.getInstance();
	private IVMManagerFactory vmmf = VMManagerFactory.getInstance();
	
	public void specializedProcess(Event e) {
		IJavaNodeManager vmm = 
            (IJavaNodeManager)vmmf.getNodeManager((String)e.request().getProperty(DebuggerConstants.VMM_KEY)).getAdapter(IJavaNodeManager.class);
		StepEvent se = (StepEvent)e;
		
		jmt.clearPreviousStepRequests(se.thread(), vmm);
	}

}
