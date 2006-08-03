/*
 * Created on Jun 5, 2006
 * 
 * file: DistributedThreadTest.java
 */
package ddproto1.debugger.managing.tracker.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.LaunchHelper;
import ddproto1.debugger.managing.ILocalNodeManager;
import ddproto1.debugger.managing.test.AbstractDebuggerTest;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.test.DDTestSuite;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestLocationConstants;
import ddproto1.util.TestUtils;
import junit.framework.TestCase;

public class DistributedThreadTest extends AbstractDebuggerTest implements IDebugEventSetListener{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(DistributedThreadTest.class);
    
    private static final String [] machineNames = {
    		"JacORB NS", "Reuters Server", "Moody Server",
    		"JP Morgan Server", "Bovespa Server"
    };
    
    private final CountDownLatch finalBpHit = new CountDownLatch(1);
    private final CountDownLatch spinBpHit = new CountDownLatch(1);
    
    private volatile IBreakpoint finalBp;
    private volatile IBreakpoint spinBp;
    
    public void testResumeSuspend()
        throws Exception
    {
        TestUtils.setPluginTest(true);

        /** We'll use the launch helper API to get this going more smoothly. */
        LaunchHelper lHelper = getLaunchHelper();
        
        List<ILocalNodeManager> applications = 
            new ArrayList<ILocalNodeManager>();        
        /** We'll add ourselves as event listeners because we'll use event information
         * for synchronization.
         */
        DebugPlugin.getDefault().addDebugEventListener(this);
        
        /** Start the global agent. */
        launchGA();
        
        /**
         * Sets the breakpoint at the last server of the call chain.
         */
        setLastBreakpoint();
        setSpinBreakpoint();
        
        /** Now start the machines:
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
                
                /** Give some time to get the stuff running (10 secs for 
                 * each node). */
                synchronized (this) {
                    try {
                        this.wait(10000);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            /** This will fire the test. */
            applications.add(lHelper.launchApplication("CORBA Client",
                    new Launch(null, "debug", null)));

            /**
             * Waits until the last breakpoint and the spin breakpoints are hit. We 
             * use separate latches to ensure that different events actually happen.
             */
            spinBpHit.await();
            finalBpHit.await();

            /**
             * There should exist two distributed threads. We'll carefully
             * inspect their state and verify correctness.
             */
            IDebugTarget globalTarget = 
                findTarget(IConfigurationConstants.CENTRAL_AGENT_CONFIG_NAME);
            
            assertTrue(globalTarget.hasThreads());
            
            IThread [] threads = globalTarget.getThreads();
            
            assertTrue(threads.length == 2);
            
            DistributedThread spinThread = 
                (DistributedThread)threads[0].getAdapter(DistributedThread.class);
            DistributedThread finalThread =
                (DistributedThread)threads[1].getAdapter(DistributedThread.class);
            
            if(spinThread.virtualStack().length() == 4){
                DistributedThread tmp = finalThread;
                finalThread = spinThread;
                spinThread = tmp;
            }
            
            assertTrue(spinThread.virtualStack().length() == 5);
            assertTrue(spinThread.virtualStack().length() == 4);
                        
        } catch (Throwable t) {
            logger.error(t);
            fail();
        } finally {
//            for(IProcess proc : lHelper.getActiveProcessesSnapshot()){
//                try{
//                    if(proc.canTerminate())
//                        proc.terminate();
//                }catch(Exception ex){
//                    logger.warn("Error while terminating process -- " + ex.getMessage());
//                }
//            }
            GODBasePlugin.getDefault().getProcessServerManager().stop();
            DebugPlugin.getDefault().removeDebugEventListener(this);
        }
    }
    
    public void testDamageDT(){ }
    
    /** PLEASE note that these are coupled to the actual implementation
     * of the test apps. If the implementation changes, this will have
     * to change as well. 
     * 
     * @param manager
     * @throws CoreException
     */
    public void setLastBreakpoint()
    		throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DISTRIBUTED_THREAD_TEST_BKP_CL, serverProj);
        finalBp = JDIDebugModel.createLineBreakpoint(resource,
        		DISTRIBUTED_THREAD_TEST_BKP_CL, DISTRIBUTED_THREAD_TEST_FINAL_BKP_LINE, -1, -1, 0, true, null);
    }
    
    public void setSpinBreakpoint()
        throws CoreException
    {
        IJavaProject serverProj = getServerProject();
        IResource resource = getBreakpointResource(DISTRIBUTED_THREAD_TEST_BKP_CL, serverProj);
        spinBp = JDIDebugModel.createLineBreakpoint(resource,
        		DISTRIBUTED_THREAD_TEST_BKP_CL, DISTRIBUTED_THREAD_TEST_SPIN_BKP_LINE, -1, -1, 0, true, null);
    }
    
    public void handleDebugEvents(DebugEvent[] events) {
        for(DebugEvent evt : events){
            if(evt.getKind() == DebugEvent.BREAKPOINT){
                if(evt.getData() == finalBp){
                    finalBpHit.countDown(); 
                } else if(evt.getData() == spinBp){
                    spinBpHit.countDown();
                }
            }
        }
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
