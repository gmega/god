package ddproto1.debugger.eventhandler.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;

import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;

import ddproto1.debugger.eventhandler.DelegatingHandler;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.processors.AbstractEventProcessor;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.Semaphore;
import ddproto1.util.traits.JDIEventProcessorTrait.JDIEventProcessorTraitImplementor;
import junit.framework.TestCase;

public class DelegatingHandlerTest extends TestCase {

	private DelegatingHandler handler;
	
	protected void setUp() throws Exception {
		handler = new DelegatingHandler();
	}
	
	public void testTypeEventHandlers()
		throws Exception
	{
		IJDIEventProcessor bpProcessor = new FailIfWrongTypeProcessor();
		
		handler.addEventListener(IEventManager.BREAKPOINT_EVENT, bpProcessor);
		handler.handleBreakpointEvent(defineEventWithPolicy(EventRequest.SUSPEND_EVENT_THREAD));
		handler.removeEventListener(IEventManager.BREAKPOINT_EVENT, bpProcessor);
	}
	
	public void testTypeEventHandlerFilters()
		throws Exception
	{
		List <FailIfWrongPolicyProcessor> processors = 
			new ArrayList<FailIfWrongPolicyProcessor>();
		int [] policyTypes = new int [] { EventRequest.SUSPEND_ALL, 
										EventRequest.SUSPEND_EVENT_THREAD, 
										EventRequest.SUSPEND_NONE };		
		
		for(int i = 0; i < policyTypes.length; i++){
			FailIfWrongPolicyProcessor processor = new FailIfWrongPolicyProcessor(policyTypes[i], handler, IEventManager.BREAKPOINT_EVENT);
			Set<Integer> policyFilter = new HashSet<Integer>();
			policyFilter.add(policyTypes[i]);
			processors.add(processor);

			handler.addEventListener(IEventManager.BREAKPOINT_EVENT, processor);
			handler.setListenerPolicyFilters(processor, policyFilter);
		}
		
		for(int i = 0; i < policyTypes.length; i++)
			handler.handleBreakpointEvent(defineEventWithPolicy(policyTypes[i]));
		
		for(FailIfWrongPolicyProcessor processor : processors){
			assertTrue(processor.called());
		}
	}
	
	private BreakpointEvent defineEventWithPolicy(int policy){
		BreakpointEvent bEvent = EasyMock.createMock(BreakpointEvent.class);
		BreakpointRequest eRequest = EasyMock.createMock(BreakpointRequest.class);

		EasyMock.expect(bEvent.request()).andReturn(eRequest);
		EasyMock.expectLastCall().anyTimes();
		EasyMock.expect(eRequest.suspendPolicy()).andReturn(policy);
		EasyMock.expectLastCall().anyTimes();
		EasyMock.replay(eRequest);
		EasyMock.replay(bEvent);
		
		return bEvent;
	}
	
	public void testRequestEventHandlers(){
		BreakpointEvent evt = defineEventWithPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		FailIfWrongRequestHandler processor = new FailIfWrongRequestHandler(evt.request(), handler);
		
		handler.addEventListener(evt.request(), processor);
		handler.handleBreakpointEvent(evt);
		assertTrue(processor.called());
	}
	
	public void testSelfRemovalThrowsException()
		throws Exception
	{
		BreakpointEvent evt = defineEventWithPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		
		handler.addEventListener(IEventManager.BREAKPOINT_EVENT, new AbstractEventProcessor (){
			public void specializedProcess(Event request) {
				try {
					handler.removeEventListener(IEventManager.BREAKPOINT_EVENT, this);
				} catch (IllegalAttributeException e) {
					fail();
				}
			}
		});
		
		try{
			handler.handleBreakpointEvent(evt);
		}catch(IllegalStateException ex){ }
	}
	
	public void testCrossRemovalDoesntThrowException(){
		
	}
	
	class FailIfWrongTypeProcessor extends AbstractEventProcessor {
		public void specializedProcess(Event request) {
			if(!(request instanceof BreakpointEvent))
				fail();
		}
	}
	
	class FailIfWrongPolicyProcessor extends AbstractEventProcessor{
		
		private int definedPolicy;
        private int fType;
        private IEventManager fMgr;
		private boolean called = false;
		
		public FailIfWrongPolicyProcessor(int policy, IEventManager mgr, int type){ 
            this.definedPolicy = policy;
            fType = type;
            fMgr = mgr;
        }
		
		public void specializedProcess(Event request){
			if(!(request.request().suspendPolicy() == definedPolicy))
				fail();
			
			called = true;
            try{
                fMgr.removeEventListener(fType, this);
            }catch(Exception ex){
                fail();
            }
		}
		
		public boolean called(){ return called; }
	}
	
	class FailIfWrongRequestHandler extends AbstractEventProcessor{
		private EventRequest request;
        private IEventManager fMgr;
		private boolean called = false;
		
		public FailIfWrongRequestHandler(EventRequest req, 
                IEventManager mgr){ 
            request = req; 
            fMgr = mgr;
		}

		public void specializedProcess(Event evt) {
			if(request != evt.request())
				fail();
			called = true;
            fMgr.removeEventListener(request, this);
		}
		
		public boolean called(){ return called; }
	}
	
	class BlockingProcessor implements JDIEventProcessorTraitImplementor{

		private IJDIEventProcessor processor;
		private boolean enabled;
		
		private Semaphore blocker = new Semaphore(0);
		
		public void specializedProcess(Event request) {
			blocker.p();
		}
		
		public void release(){
			blocker.v();
		}

		public void next(IJDIEventProcessor next) { processor = next; }
		public IJDIEventProcessor next() { return processor; }
		public boolean enabled() { return enabled; }
		public void enabled(boolean newValue) { enabled = newValue; }
		
	}

}
