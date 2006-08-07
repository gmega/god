/*
 * Created on Jun 5, 2006
 * 
 * file: DistributedThreadTest.java
 */
package ddproto1.debugger.managing.tracker.tests;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.testplugin.DebugThreadPartOfIDEventWaiter;
import org.eclipse.jdt.debug.testplugin.DebugBreakpointThreadNameWaiter;
import org.eclipse.jdt.debug.testplugin.DebugElementKindEventDetailWaiter;
import org.eclipse.jdt.debug.testplugin.SpecificDebugElementEventWaiter;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.test.AbstractDebuggerTest;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.NilDistributedThread;
import ddproto1.util.MessageHandler;

public class DistributedThreadTest extends AbstractDebuggerTest{
    
    private static final int NODE_STARTUP = 6000;
    private static final Logger logger = MessageHandler.getInstance().getLogger(DistributedThreadTest.class);
    
    private static final String [] machineNames = {
    		"JacORB NS", "Reuters Server" };/*, "Moody Server",
    		"JP Morgan Server", "Bovespa Server"
    };*/
    
    private static volatile IThread fDistributedThread;
    
    public void testSetUp() throws Exception{
        configureEverything();
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
        try {
            for (String name : machineNames) {

                launchApplication(name);
                
                /** Give some time to get the stuff running. */
                synchronized (this) {
                    try {
                        this.wait(NODE_STARTUP);
                    } catch (InterruptedException ex) {
                    }
                }
            }
                        
        } catch (Throwable t) {
            logger.error(t);
            fail();
        } 
        
    }
    
    public void testDistributedStepInto() throws Exception{
        /** Sets the client-side breakpoint at runnable requester. */
        IJavaProject clientProject = getClientProject();
        IResource sResource = getBreakpointResource(DT_TEST_CLIENT_BKP_CL, clientProject);
        IBreakpoint clientBkp = JDIDebugModel.createLineBreakpoint(sResource,
                DT_TEST_CLIENT_BKP_CL, DT_TEST_CLIENT_BKP_LINE, -1, -1, 0,
                true, null);
        
        /** Waits until a thread with the given name hits this breakpoint */
        DebugBreakpointThreadNameWaiter waiter = 
            new DebugBreakpointThreadNameWaiter(DebugEvent.SUSPEND, clientBkp, DT_TEST_STEP_INTO_THREAD_NAME);
        
        /** Fires the client. */
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
        
        clientSide.stepInto();
        
        localStepStart.waitForEvent();  // Local thread started stepping
        localResumed.waitForEvent();    // Local thread resumed as it entered a stub.
        remoteStepStart.waitForEvent(); // Distributed thread started stepping
        serverThreadBkp.waitForEvent(); // Server-side local thread hit breakpoint
        remoteStepEnd.waitForEvent();   // Distributed thread stopped stepping

        assertNotNull(remoteStepEnd.getEvent());
        assertTrue(remoteStepEnd.getEvent().getSource() instanceof DistributedThread);

        fDistributedThread = (DistributedThread)remoteStepEnd.getEvent().getSource();
    }
    
    public void testDistributedStepOver() throws Exception{
        setLastBreakpoint();
    }

    public void testDistributedStepReturn() throws Exception{
        
    }
    
    public void testResumeSuspend()
        throws Exception
    {
//        /** Start the global agent. */
//        launchGA();
//        
//        /**
//         * Sets the breakpoint at the last server of the call chain.
//         */
//        setSpinBreakpoint();
//
//
//        /**
//         * There should exist two distributed threads. We'll carefully
//         * inspect their state and verify correctness.
//         */
//        IDebugTarget globalTarget = 
//            findTarget(IConfigurationConstants.CENTRAL_AGENT_CONFIG_NAME);
//        
//        assertTrue(globalTarget.hasThreads());
//        
//        IThread [] threads = globalTarget.getThreads();
//        
//        assertTrue(threads.length == 2);
//        
//        DistributedThread spinThread = 
//            (DistributedThread)threads[0].getAdapter(DistributedThread.class);
//        DistributedThread finalThread =
//            (DistributedThread)threads[1].getAdapter(DistributedThread.class);
//        
//        if(spinThread.virtualStack().length() == 4){
//            DistributedThread tmp = finalThread;
//            finalThread = spinThread;
//            spinThread = tmp;
//        }
//        
//        assertTrue(spinThread.virtualStack().length() == 5);
//        assertTrue(spinThread.virtualStack().length() == 4);
        
    }
    
    public void testDamageDistributedThread(){ }
    
    public void testTearDown() throws Exception{
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
        return JDIDebugModel.createLineBreakpoint(resource,
        		DT_TEST_SERVER_BKP_CL, DT_TEST_FINAL_BKP_LINE, -1, -1, 0, true, null);
    }
    
    /** Sets a breakpoint that will be triggered in the spinning thread of the
     * call chain. We'll use this for testing resume and suspend.
     */
    public IBreakpoint setSpinBreakpoint()
        throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DT_TEST_SERVER_BKP_CL, serverProj);
        return JDIDebugModel.createLineBreakpoint(resource,
        		DT_TEST_SERVER_BKP_CL, DT_TEST_SPIN_BKP_LINE, -1, -1, 0, true, null);
    }
    
    private IDebugTarget findTarget(String name){
        IDebugTarget [] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
        try{
            for(IDebugTarget target : targets){
                if(target.getName().equals(name))
                    return target;
            }
        }catch(Exception ex){ }
        
        fail();
        return null;
    }
   
}
