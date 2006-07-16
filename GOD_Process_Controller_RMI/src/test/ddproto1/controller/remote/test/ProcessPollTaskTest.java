/*
 * Created on Jun 18, 2006
 * 
 * file: ProcessPollTaskTest.java
 */
package ddproto1.controller.remote.test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.easymock.EasyMock;

import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import ddproto1.controller.remote.impl.ProcessPollTask;

public class ProcessPollTaskTest extends TestCase{
    
    private static final int POLL_THREADS = 5;
    
    private final ScheduledExecutorService executor = 
        new ScheduledThreadPoolExecutor(POLL_THREADS);
    
    private ScheduledFuture sf;
    
    private volatile boolean notified = false;
    
    /**
     * Please note that this test might not terminate. This should be considered a failure
     * as well.
     * 
     * @throws Exception
     */
    public void testTerminationDetection()
        throws Exception
    {
        BasicConfigurator.configure();
        LaunchParametersDTO lp = TestUtils.crankDefaultLaunch(0, 10000, 0);
        
        IControlClient cClient = EasyMock.createMock(IControlClient.class);
        cClient.notifyProcessDeath(0, 0);
        EasyMock.expectLastCall().once();
        
        
        EasyMock.replay(cClient);
        
        ProcessPollTask ppTask = new
            ProcessPollTask(cClient, Runtime.getRuntime().exec(lp.getCommandLine()), 0);
        
        synchronized(this){
            ppTask.scheduleOnExecutor(executor, 100, TimeUnit.MILLISECONDS);
        }
        
        synchronized(this){
            while(!ppTask.isDone())
                this.wait(200);
        }
        
        EasyMock.verify(cClient);
    }
    
}
