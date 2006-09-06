/*
 * Created on May 8, 2006
 * 
 * file: DeferrableBreakpointRequestTest.java
 */
package ddproto1.debugger.request.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.easymock.EasyMock;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IJavaNodeManagerRegistry;
import ddproto1.debugger.managing.INodeManager;
import ddproto1.debugger.managing.INodeManagerRegistry;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.DeferrableClassPrepareRequest;
import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdResolutionContextImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.util.ILogManager;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

public class DeferrableBreakpointRequestTest extends TestCase {
    
    private static final String VMID = "TestVM";
    private static final int LINE_NO = 50;
    
    private DeferrableRequestQueue drq;
    private ReferenceType outerClass;
    private ReferenceType innerClass;
    private List<ReferenceType>classList = new ArrayList<ReferenceType>();
    
    private ClassPrepareRequest nonPatterned;
    private ClassPrepareRequest patterned;
    private BreakpointRequest bkp;
    
    private IJavaNodeManager vmm;
    private Location loc;
    private IJavaNodeManagerRegistry vmmf;
    private EventRequestManager erm;
    private VirtualMachine vm;
    
    public void setUp()
        throws Exception
    {
        
        drq = new DeferrableRequestQueue();
        
        /** Creates a fake outer class, */
        outerClass = EasyMock.createMock(ReferenceType.class);
        EasyMock.expect(outerClass.name()).andReturn("com.test.SomeClass");
        EasyMock.expectLastCall().atLeastOnce();
        
        EasyMock.expect(outerClass.locationsOfLine(LINE_NO)).andReturn(new ArrayList());
        EasyMock.expectLastCall().atLeastOnce();
        
        /** a fake inner class, */
        innerClass = EasyMock.createMock(ReferenceType.class);
        EasyMock.expect(innerClass.name()).andReturn("com.test.SomeClass$1");
        EasyMock.expectLastCall().atLeastOnce();
        
        Location loc = EasyMock.createMock(Location.class);
        List<Location> locations = new ArrayList<Location>();
        locations.add(loc);
        
        EasyMock.expect(innerClass.locationsOfLine(LINE_NO)).andReturn(locations);
        EasyMock.expectLastCall().atLeastOnce();

        
        /** a fake VirtualMachine and EventRequestManager, */
        erm = EasyMock.createMock(EventRequestManager.class);
        vm = EasyMock.createMock(VirtualMachine.class);
        EasyMock.expect(vm.eventRequestManager()).andReturn(erm);
        EasyMock.expectLastCall().atLeastOnce();
        EasyMock.expect(vm.allClasses()).andReturn(classList);
        EasyMock.expectLastCall().atLeastOnce();
        EasyMock.expect(vm.classesByName("com.test.SomeClass")).andReturn(classList);
        EasyMock.expectLastCall().atLeastOnce();
        
        /** a fake VirtualMachineManager, */
        vmm = EasyMock.createMock(IJavaNodeManager.class);
        EasyMock.expect(vmm.getDeferrableRequestQueue()).andReturn(drq);
        EasyMock.expectLastCall().atLeastOnce();
        EasyMock.expect(vmm.virtualMachine()).andReturn(vm);
        EasyMock.expectLastCall().atLeastOnce();
        EasyMock.expect(vmm.getName()).andReturn(VMID);
        EasyMock.expectLastCall().atLeastOnce();
        
        /** and a fake VMManagerFactory. */
        vmmf = EasyMock.createMock(IJavaNodeManagerRegistry.class);
        EasyMock.expect(vmmf.getJavaNodeManager(VMID)).andReturn(vmm);
        EasyMock.expectLastCall().atLeastOnce();
        
        VMManagerFactory.setInstance(vmmf);
        
        /** During breakpoint placement, two ClassPrepareRequests and two
         * breakpoint requests have to be created. */
        // This ClassPrepareRequest refers to the outer class.
        nonPatterned = EasyMock.createMock(ClassPrepareRequest.class);
        EasyMock.expect(erm.createClassPrepareRequest()).andReturn(nonPatterned);
        EasyMock.expectLastCall().once();
        nonPatterned.addClassFilter("com.test.SomeClass");
        EasyMock.expectLastCall().once();
        nonPatterned.addCountFilter(1);
        EasyMock.expectLastCall().once();
        nonPatterned.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        EasyMock.expectLastCall().once();
        nonPatterned.enable();
        EasyMock.expectLastCall().once();

        // This ClassPrepareRequest refers to the inner class.        
        patterned = EasyMock.createMock(ClassPrepareRequest.class);
        EasyMock.expect(erm.createClassPrepareRequest()).andReturn(patterned);
        EasyMock.expectLastCall().once();
        patterned.addClassFilter("com.test.SomeClass*");
        EasyMock.expectLastCall().once();
        patterned.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        EasyMock.expectLastCall().once();
        patterned.enable();
        EasyMock.expectLastCall().once();
        
        /** Finally, we emulate the actual breakpoint placement. */
        bkp = EasyMock.createMock(BreakpointRequest.class);
        bkp.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        EasyMock.expectLastCall().once();
        bkp.putProperty(DebuggerConstants.VMM_KEY, VMID);
        EasyMock.expectLastCall().once();
        bkp.enable();
        EasyMock.expectLastCall().once();
        
        EasyMock.expect(erm.createBreakpointRequest(loc)).andReturn(bkp);
        EasyMock.expectLastCall().once();
        
        // Cancelling must imply that all requests be removed.
        erm.deleteEventRequest(nonPatterned);
        EasyMock.expectLastCall().once();
        
        erm.deleteEventRequest(patterned);
        EasyMock.expectLastCall().once();
        
        erm.deleteEventRequest(bkp);
        EasyMock.expectLastCall().once();
                       
        EasyMock.replay(outerClass, innerClass, erm, vm, vmm, vmmf, patterned, nonPatterned, bkp);
        
        /** Our little disgusting hack. */
        DeferrableClassPrepareRequest.registerVM(VMID);
    }
    
    public void testAbsoluteInnerClassBreakpoint()
        throws Exception
    {
        DeferrableBreakpointRequest innerBreakpoint = 
            new DeferrableBreakpointRequest(VMID, "com.test.SomeClass", LINE_NO);
        
        ResolutionListener rl = new ResolutionListener(innerBreakpoint);
        
        IJavaNodeManager vmm = 
            (IJavaNodeManager)VMManagerFactory.getRegistryManagerInstance().getJavaNodeManager(VMID);
        
        vmm.getDeferrableRequestQueue().addEagerlyResolve(innerBreakpoint);
        
        vmm.getDeferrableRequestQueue().resolveForContext(
                createResolutionContext(null,
                        IDeferrableRequest.VM_CONNECTION,
                        IDeferrableRequest.MATCH_ONCE, vmm.virtualMachine()));
        
        vmm.getDeferrableRequestQueue().resolveForContext(
                createResolutionContext("com.test.SomeClass",
                        IDeferrableRequest.CLASSLOADING,
                        IDeferrableRequest.MATCH_ONCE, outerClass));

        classList.add(outerClass);
        
        assertFalse(rl.hasBeenHit());
        
        vmm.getDeferrableRequestQueue().resolveForContext(
                createResolutionContext("com.test.SomeClass$1",
                        IDeferrableRequest.CLASSLOADING,
                        IDeferrableRequest.MATCH_ONCE, innerClass));
        
        assertTrue(rl.hasBeenHit());
        
        drq.removeRequest(innerBreakpoint);
        
        EasyMock.verify(outerClass, innerClass, erm, vm, vmm, vmmf, patterned, nonPatterned, bkp);
    }
    
    public void tearDown(){
        VMManagerFactory.setInstance(VMManagerFactory.getInstance());
    }
    
    private IDeferrableRequest.IResolutionContext createResolutionContext(String clsName, int eventType, int matchType, Object ctxContents){
        StdPreconditionImpl precond = new StdPreconditionImpl();
        precond.setClassId(clsName);
        precond.setType(new StdTypeImpl(eventType, matchType));
        StdResolutionContextImpl ctx = new StdResolutionContextImpl();
        ctx.setContext(ctxContents);
        ctx.setPrecondition(precond);
        
        return ctx;
    }
    
    private class ResolutionListener implements IResolutionListener{
        private boolean hasBeenHit;
        private DeferrableBreakpointRequest dbr;

        public ResolutionListener(DeferrableBreakpointRequest dbr){
            this.dbr = dbr;
            dbr.addResolutionListener(this);
        }
        
        public void notifyResolution(IDeferrableRequest source, Object byproduct) {
            hasBeenHit = true;
            assertTrue(byproduct == bkp);
            assertTrue(source == dbr);
        }
        
        public boolean hasBeenHit() { return hasBeenHit; }
    }
}
