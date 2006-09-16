/*
 * Created on 9/09/2006
 * 
 * file: StackframeValidityTest.java
 */
package ddproto1.debugger.managing.tracker;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.testplugin.DebugBreakpointThreadNameWaiter;
import org.eclipse.jdt.debug.testplugin.SpecificDebugElementEventWaiter;

import com.sun.jdi.Location;

import ddproto1.debugger.managing.JavaStackframe;
import ddproto1.debugger.managing.test.AbstractDebuggerTest;
import ddproto1.util.TestLocationConstants;

public class StackframeValidityTest extends AbstractDebuggerTest implements TestLocationConstants{
    private static final String[] machineNames = { "JacORB NS",
            "Reuters Server" };
    
    public void testStartDistributedSystem() throws Exception{
        launchGA();
        launchApplications(machineNames);
    }
    
    public void testStackframeValidity() throws Exception{
        /** Places a breakpoint at the beginning of the Client's
         * run method. It's enough to reveal the bug. */
        IResource clientClass = getBreakpointResource(CLIENT_BKP_CL, getClientProject());
        IBreakpoint clientBkp = ensureCreateBreakpoint(clientClass, CLIENT_BKP_CL, SFV_TEST_CLIENT_BKP_LINE, -1, -1, 0, true, null);
        
        /** Waits for both client threads to hit the same breakpoint. */
        DebugBreakpointThreadNameWaiter t1Waiter = 
            new DebugBreakpointThreadNameWaiter(DebugEvent.SUSPEND, clientBkp, CLIENT_THREAD1_NAME);
        DebugBreakpointThreadNameWaiter t2Waiter = 
            new DebugBreakpointThreadNameWaiter(DebugEvent.SUSPEND, clientBkp, CLIENT_THREAD2_NAME);

        /** Launches the client */
        launchApplication("CORBA Client");
        
        t1Waiter.waitForEvent();
        t2Waiter.waitForEvent();
        
        assertNotNull(t1Waiter.getSuspendedThread());
        assertNotNull(t2Waiter.getSuspendedThread());
        
        IThread jThreadOne = t1Waiter.getSuspendedThread();
        IThread jThreadTwo = t2Waiter.getSuspendedThread();
        
        assertNotNull(jThreadOne);
        assertTrue(jThreadOne.isSuspended());
        assertNotNull(jThreadTwo);
        assertTrue(jThreadTwo.isSuspended());
        
        try{
            verifyLocation(jThreadOne, CLIENT_BKP_CL, SFV_TEST_CLIENT_BKP_LINE);
            verifyLocation(jThreadTwo, CLIENT_BKP_CL, SFV_TEST_CLIENT_BKP_LINE);
            IStackFrame twoSFrame = jThreadTwo.getTopStackFrame();
            stepOver(jThreadOne);
            verifyLocation(jThreadOne, CLIENT_BKP_CL, SFV_TEST_CLIENT_AFTER_STEP_LINE);
            verifyFrameLocation(twoSFrame, CLIENT_BKP_CL, SFV_TEST_CLIENT_BKP_LINE);
        }catch(Exception ex){
            logger.error(ex);
            fail();
        }
    }
    
    public void testShutDown() throws Exception{
        performDistributedShutdown();
    }
    
    private void verifyLocation(IThread thread, String type, int line)
        throws Exception
    {
        verifyFrameLocation(thread.getTopStackFrame(), type, line);
        
        IStackFrame [] frames = thread.getStackFrames();
        for(IStackFrame frame : frames)
            getLocationFromFrame(frame);
    }
    
    private Location getLocationFromFrame(IStackFrame sf)
        throws Exception
    {
        JavaStackframe jsf = (JavaStackframe)sf.getAdapter(JavaStackframe.class);
        return jsf.getLocation();
    }
    
    private void verifyFrameLocation(IStackFrame sFrame, String type, int line)
        throws Exception
    {
        Location first = getLocationFromFrame(sFrame);
        assertTrue(first.declaringType().name().startsWith(type));
        assertTrue("Expected: " + line + " but got: " + first.lineNumber(), first.lineNumber() == line);
    }
    
    private void stepOver(IThread tr)
        throws Exception
    {
        SpecificDebugElementEventWaiter resume = new SpecificDebugElementEventWaiter(DebugEvent.RESUME, tr);
        SpecificDebugElementEventWaiter suspend = new SpecificDebugElementEventWaiter(DebugEvent.SUSPEND, tr);
        assertTrue(tr.canStepOver());
        tr.stepOver();
        resume.waitForEvent();
        suspend.waitForEvent();
        assertTrue(resume.getEvent().getDetail() == DebugEvent.STEP_OVER);
        assertTrue(suspend.getEvent().getDetail() == DebugEvent.STEP_END);
    }
}
