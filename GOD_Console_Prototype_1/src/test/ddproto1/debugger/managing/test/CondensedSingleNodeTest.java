/*
 * Created on Jun 5, 2006
 * 
 * file: JavaThreadTest.java
 */
package ddproto1.debugger.managing.test;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.testplugin.DebugBreakpointWaiter;
import org.eclipse.jdt.debug.testplugin.SpecificDebugElementEventWaiter;

import ddproto1.GODBasePlugin;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestUtils;


/**
 * This test is a rather superficial test for basic symbolic 
 * debugging functionality. It will set a breakpoint on a well-
 * know application, launch it, and wait for that breakpoint to be hit. 
 * It will then perform a step into, a step over and a step return,
 * checking thread state as each step occurs. After that, the test
 * will remove the breakpoint and resume the main thread, and will
 * wait for the application to die. If the application dies within
 * the alloted timeframe, the test succeeds.
 * 
 * @author giuliano
 *
 */
public class CondensedSingleNodeTest extends AbstractDebuggerTest{

    private static volatile IThread fThread;
    private static volatile IBreakpoint fLineBkp;
    
    /** Adds breakpoint, launches application and waits for breakpoint 
     * to be hit. */
    public void testLineBreakpoint()
        throws Exception
    {
        configureEverything();
        IJavaProject miscProj = getMiscProject();
        IResource resource = getBreakpointResource(
                CSN_TEST_LOOP_BKP_CL, miscProj);
        fLineBkp = JDIDebugModel.createLineBreakpoint(resource,
                CSN_TEST_LOOP_BKP_CL, CSN_TEST_BKP_LINE,
                -1, -1, 0, true, null);
        
        DebugBreakpointWaiter dbw = 
            new DebugBreakpointWaiter(DebugEvent.SUSPEND, fLineBkp);
        
        launchGA();
        launchApplication(TestUtils.getProperty(MISC_PROJECT_NAME));
            
        dbw.waitForEvent();
        fThread = dbw.getSuspendedThread();
        assertNotNull(fThread);
    }
    
    /** Tests step into. */
    public void testStepInto()
        throws Exception
    {
        assertTrue(fThread.isSuspended());
        assertFalse(fThread.isStepping());
        assertTrue(fThread.canStepInto());
        doStep(DebugEvent.STEP_INTO, 3, CSN_TEST_STEPINTO_LINE,
                new Stepper(){
                    public void step() throws Exception {
                        fThread.stepInto();
                    }
                });
    }
    
    /** Tests step return */
    public void testStepReturn()
        throws Exception
    {
        assertTrue(fThread.isSuspended());
        assertFalse(fThread.isStepping());
        assertTrue(fThread.canStepReturn());
        doStep(DebugEvent.STEP_RETURN, 2, CSN_TEST_BKP_LINE + 1,
                new Stepper(){
                    public void step() throws Exception {
                        fThread.stepReturn();
                    }
                });
    }
    
    /** Tests step over */
    public void testStepOver() throws Exception{
        assertTrue(fThread.isSuspended());
        assertFalse(fThread.isStepping());
        assertTrue(fThread.canStepReturn());
        doStep(DebugEvent.STEP_OVER, 2, CSN_TEST_BKP_LINE - 2,
                new Stepper(){
                    public void step() throws Exception {
                        fThread.stepOver();
                    }
                });
    }
    
    /** Cancels breakpoint and resumes current thread. Application should
     * die shortly in less than DebugEventWaiter.DEFAULT_TIMEOUT
     */
    public void testCancellation()
        throws Exception
    {
        BreakpointEventWaiter waiter = 
            new BreakpointEventWaiter(BreakpointEventWaiter.REMOVE, fLineBkp);
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(fLineBkp, true);
        
        waiter.waitForEvent();
        
        /** Now resumes the thread and waits for the program to die. */
        IDebugTarget dTarget = fThread.getDebugTarget();
        SpecificDebugElementEventWaiter deathWaiter = 
            new SpecificDebugElementEventWaiter(DebugEvent.TERMINATE, dTarget);
        
        assertTrue(fThread.isSuspended());
        assertFalse(fThread.isStepping());
        
        fThread.resume();
        
        deathWaiter.waitForEvent();
        assertNotNull(deathWaiter.getEvent());
    }
    
    public void testTearDown()
        throws Exception
    {
        performDistributedShutdown();
    }
    
    /** Actually performs each step request. */
    private void doStep(int stepKind, int stackLength, int expectedLine, Stepper stepper) 
        throws Exception{
        SpecificDebugElementEventWaiter waiterStart = 
            new SpecificDebugElementEventWaiter(DebugEvent.RESUME,
                    fThread);
        
        SpecificDebugElementEventWaiter waiterEnd = 
            new SpecificDebugElementEventWaiter(DebugEvent.SUSPEND,
                    fThread);
        
        /** Fires the step into */
        stepper.step();
        
        /** It should start... */
        waiterStart.waitForEvent();
        assertTrue(waiterStart.getEvent().getDetail() == stepKind);
        
        /** ... and it should end. */
        waiterEnd.waitForEvent();
        assertTrue(waiterEnd.getEvent().getDetail() == DebugEvent.STEP_END);

        /** Verifies that the thread is where it should be. */
        IThread thread = (IThread) waiterStart.getEvent().getSource();
        IStackFrame [] frames = thread.getStackFrames();
        assertTrue(frames.length == stackLength);
        IStackFrame topFrame = thread.getTopStackFrame();
        assertTrue(topFrame.getLineNumber() == expectedLine);
    }
    
    interface Stepper{
        public void step() throws Exception;
    }
}
