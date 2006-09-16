/*
 * Created on Jun 5, 2006
 * 
 * file: DistributedThreadTest.java
 */
package ddproto1.debugger.managing.tracker;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.testplugin.DebugThreadPartOfIDEventWaiter;
import org.eclipse.jdt.debug.testplugin.DebugBreakpointThreadNameWaiter;
import org.eclipse.jdt.debug.testplugin.DebugElementKindEventDetailWaiter;
import org.eclipse.jdt.debug.testplugin.SpecificDebugElementEventWaiter;

import com.sun.jdi.Location;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.INodeManager;
import ddproto1.debugger.managing.JavaStackframe;
import ddproto1.debugger.managing.test.AbstractDebuggerTest;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;
import ddproto1.util.MessageHandler;

public class DistributedThreadControlTest extends AbstractDebuggerTest{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(DistributedThreadControlTest.class);
    
    private static final String [] machineNames = {
    		"JacORB NS", "Reuters Server" /*};/* */, "Moody Server",
    		"JP Morgan Server", "Bovespa Server"
    };/**/
    
    private static volatile DistributedThread fDistributedThread;
    
    public void testSetUp() throws Exception{
        launchGA();
        startDistributedSystem();
    }

    /** 
     * Starts all machines with the exception of the Client.
     * These machines will be used by the remaining tests.  
     * 
     * @throws Exception
     */
    private void startDistributedSystem() throws Exception{
        /** Start the machines:
         * - Name Service
         * - Reuters
         * - Moody 
         * - JP Morgan:
         *      Will enter an infinite loop, giving us opportunity 
         *      to suspend the DThread when we actually know where
         *      it is.
         * - Bovespa:
         *      Will hit a breakpoint. 
         */
        launchApplications(machineNames);
        
    }
    
    public void testDistributedStepInto() throws Exception{
        /** Sets the client-side breakpoint at runnable requester. */
        IJavaProject clientProject = getClientProject();
        IResource sResource = getBreakpointResource(CLIENT_BKP_CL, clientProject);
        IBreakpoint clientBkp = JDIDebugModel.createLineBreakpoint(sResource,
                CLIENT_BKP_CL, DT_TEST_CLIENT_BKP_LINE, -1, -1, 0,
                true, null);
        
        /** Waits until a thread with the given name hits this breakpoint */
        DebugBreakpointThreadNameWaiter waiter = 
            new DebugBreakpointThreadNameWaiter(DebugEvent.SUSPEND, clientBkp, CLIENT_THREAD1_NAME);
        
        /** Fires the client. */
        Thread.sleep(LAUNCH_DELAY); // Stalls for safety.
        launchApplication("CORBA Client");
        
        waiter.waitForEvent();
              
        assertTrue(waiter.getSuspendedThread() instanceof ILocalThread);
        ILocalThread clientSide = (ILocalThread)waiter.getSuspendedThread();
        
        /** Performs a step into. This step into will generate five events. */
        SpecificDebugElementEventWaiter localStepStart = 
            new SpecificDebugElementEventWaiter(DebugEvent.RESUME, clientSide);
        SpecificDebugElementEventWaiter localResumed = 
            new SpecificDebugElementEventWaiter(DebugEvent.RESUME, clientSide);
        DebugElementKindEventDetailWaiter remoteStepStart = 
            new DebugElementKindEventDetailWaiter(DebugEvent.RESUME, DistributedThread.class, DebugEvent.STEP_INTO);
        DebugThreadPartOfIDEventWaiter serverThreadBkp = 
            new DebugThreadPartOfIDEventWaiter(DebugEvent.SUSPEND, clientSide.getGUID());
        DebugElementKindEventDetailWaiter remoteStepEnd = 
            new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, DistributedThread.class, DebugEvent.STEP_END);
        
        assertFalse(clientSide.isStepping());
        INodeManager globalAgent = (INodeManager)findTarget(CENTRAL_AGENT_NAME).
            getAdapter(INodeManager.class);

        /** Casts thread reference into distributed thread. */
        IThread tReference =
            globalAgent.getThreadManager().getThread(clientSide.getGUID());
        assertNotNull(tReference);
        fDistributedThread = (DistributedThread)tReference.getAdapter(DistributedThread.class);        
        assertFalse(fDistributedThread.isStepping());
        
        clientSide.stepInto();
        
        localStepStart.waitForEvent();  // Local thread started stepping
        localResumed.waitForEvent();    // Local thread resumed as it entered a stub.
        remoteStepStart.waitForEvent(); // Distributed thread started stepping
        serverThreadBkp.waitForEvent(); // Server-side local thread hit breakpoint
        remoteStepEnd.waitForEvent();   // Distributed thread stopped stepping

        assertNotNull(remoteStepEnd.getEvent());
        
        DistributedThread dThread = (DistributedThread)fDistributedThread.getAdapter(DistributedThread.class);
        assertNotNull(dThread);
        verifyLocation(dThread, 2, DT_TEST_SERVER_BKP_CL, DT_TEST_FIRST_QUOTERIMPL_EXECUTABLE_LINE);
    }
    
    public void testDistributedStepOver() throws Exception{
        /** Sets a breakpoint just before the next upcall */
        IBreakpoint callBkp = setCallBreakpoint();
        SpecificDebugElementEventWaiter dThreadBreakpointHit = 
            new SpecificDebugElementEventWaiter(DebugEvent.SUSPEND, fDistributedThread);
        fDistributedThread.resume();
        dThreadBreakpointHit.waitForEvent();
        assertTrue(dThreadBreakpointHit.getEvent().getDetail() == DebugEvent.BREAKPOINT);
        callBkp.delete();
        
        /** We'll step over, but there's a breakpoint in the middle of the path. 
         * It will cause the thread to stop. */
        setLastBreakpoint();
        SpecificDebugElementEventWaiter distributedStepStart = 
            new SpecificDebugElementEventWaiter(DebugEvent.RESUME, fDistributedThread);
        SpecificDebugElementEventWaiter breakpointHit = 
            new SpecificDebugElementEventWaiter(DebugEvent.SUSPEND, fDistributedThread);
        
        fDistributedThread.stepOver();
        distributedStepStart.waitForEvent();
        breakpointHit.waitForEvent();
        
        verifyLocation(fDistributedThread, 5, DT_TEST_SERVER_BKP_CL, DT_TEST_FINAL_BKP_LINE);
    }

    public void testDistributedStepReturn() throws Exception{
        /** We'll step return. An exception will be thrown and this distributed
         * thread should stop at the first machine's exception handler.
         */
        SpecificDebugElementEventWaiter distributedStepStart = 
            new SpecificDebugElementEventWaiter(DebugEvent.RESUME, fDistributedThread);
        SpecificDebugElementEventWaiter distributedStepEnd = 
            new SpecificDebugElementEventWaiter(DebugEvent.SUSPEND, fDistributedThread);
        
        fDistributedThread.stepReturn();
        distributedStepStart.waitForEvent();
        assertNotNull(distributedStepStart.getEvent());
        assertTrue(distributedStepStart.getEvent().getDetail() == DebugEvent.STEP_RETURN);
        distributedStepEnd.waitForEvent();
        assertNotNull(distributedStepEnd.getEvent());
        assertTrue(distributedStepEnd.getEvent().getDetail() == DebugEvent.STEP_END);
        verifyLocation(fDistributedThread, 1, CLIENT_BKP_CL, DT_TEST_CLIENT_EXCEPTIONHANDLER_LINE);
    }
    
    public void testDistributedThreadDeath() throws Exception{
        
    }
    
//    public void testResumeSuspend()
//        throws Exception
//    {
//        /**
//         * Sets the breakpoint at the last server of the call chain.
//         */
//        setSpinBreakpoint();
//        
//    }
    
    public void testDamageDistributedThread(){  }
    
    public void testShutDown() throws Exception{
        performDistributedShutdown();
    }
        
    /** PLEASE note that these are coupled to the actual implementation
     * of the test apps. If the implementation changes, this will have
     * to change as well. 
     * 
     * @param manager
     * @throws CoreException
     */
    
    /** Sets a breakpoint that will be triggered in the last application of the
     * call chain.
     */
    public IBreakpoint setLastBreakpoint()
    		throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DT_TEST_SERVER_BKP_CL, serverProj);
        return ensureCreateBreakpoint(resource, DT_TEST_SERVER_BKP_CL, DT_TEST_FINAL_BKP_LINE, -1, -1, 0, true, 
                    null);
    }
    
    /** Sets a breakpoint that will be triggered in the spinning thread of the
     * call chain. We'll use this for testing resume and suspend.
     */
    public IBreakpoint setSpinBreakpoint()
        throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DT_TEST_SERVER_BKP_CL, serverProj);
        return ensureCreateBreakpoint(resource,
        		DT_TEST_SERVER_BKP_CL, DT_TEST_SPIN_BKP_LINE, -1, -1, 0, true, null);
    }
    
    public IBreakpoint setCallBreakpoint()
        throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DT_TEST_SERVER_BKP_CL, serverProj);
        return ensureCreateBreakpoint(resource,
                DT_TEST_SERVER_BKP_CL, DT_TEST_CHAINED_UPCALL_LINE, -1, -1, 0, true, null);
    }
    
    private void verifyLocation(DistributedThread toVerify, 
            int virtualStackSize, String typeLocation, int lineLocation)
        throws Exception
    {
        assertNotNull(toVerify);
        int actualSize = toVerify.virtualStack().getVirtualFrameCount();
        assertTrue("Expected: " + virtualStackSize + " but got: " + actualSize, virtualStackSize == actualSize);
        IStackFrame topFrame = toVerify.getTopStackFrame();
        assertTrue(topFrame.getLineNumber() == lineLocation);
        
        /** Now casts to java-specific stack frame to get the type. */
        JavaStackframe jsf = (JavaStackframe)topFrame.getAdapter(JavaStackframe.class);
        assertNotNull(jsf);
        Location jdiLocation = jsf.getLocation();
        // Use startswith instead of equals because thread might be at an inner class.
        assertTrue(jdiLocation.declaringType().name().startsWith(typeLocation));
    }
}
