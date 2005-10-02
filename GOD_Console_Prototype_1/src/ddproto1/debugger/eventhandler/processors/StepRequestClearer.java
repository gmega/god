package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.util.traits.JDIMiscTrait;

public class StepRequestClearer extends AbstractEventProcessor{

	private JDIMiscTrait jmt = JDIMiscTrait.getInstance();
	private VMManagerFactory vmmf = VMManagerFactory.getInstance();
	
	@Override
	protected void specializedProcess(Event e) {
		VirtualMachineManager vmm = vmmf.getVMManager((String)e.request().getProperty(DebuggerConstants.VMM_KEY));
		StepEvent se = (StepEvent)e;
		
		jmt.clearPreviousStepRequests(se.thread(), vmm);
	}

}
