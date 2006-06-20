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

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import ddproto1.controller.client.AMI_ControlClient_notifyProcessDeath;
import ddproto1.controller.client.ControlClientPrx;
import ddproto1.controller.client.LaunchParameters;
import ddproto1.controller.remote.impl.ProcessPollTask;

public class ProcessPollTaskTest extends TestCase{
    
    private static final int POLL_THREADS = 5;
    
    private final ScheduledExecutorService executor = 
        new ScheduledThreadPoolExecutor(POLL_THREADS);
    
    private ScheduledFuture sf;
    
    /**
     * Please note that this test might not terminate. This should be considered a failure
     * as well.
     * 
     * @throws Exception
     */
    public void testTerminationDetection()
        throws Exception
    {
        LaunchParameters lp = TestUtils.crankDefaultLaunch(0, 10000, 0);
        
        ControlClientPrx cClient = EasyMock.createMock(ControlClientPrx.class);
        cClient.notifyProcessDeath_async(EasyMock.isA(AMI_ControlClient_notifyProcessDeath.class), EasyMock.eq(0));
        EasyMock.expectLastCall().once().andAnswer(new IAnswer<Object>(){
            public Object answer() throws Throwable {
                cancelScheduledFuture();
                return null;
            }
        });
        
        
        EasyMock.replay(cClient);
        
        ProcessPollTask ppTask = new
            ProcessPollTask(cClient, Runtime.getRuntime().exec(lp.commandLine), 0);
        
        synchronized(this){
            sf = executor.scheduleAtFixedRate(ppTask, 0, 100, TimeUnit.MILLISECONDS);
        }
        
        synchronized(this){
            while(!sf.isCancelled())
                this.wait();
        }
        
        EasyMock.verify(cClient);
    }
    
    private synchronized void cancelScheduledFuture(){
        sf.cancel(true);
        this.notifyAll();
    }
}
