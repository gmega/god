/*
 * Created on Jun 18, 2006
 * 
 * file: MultiDeathTest.java
 */
package ddproto1.controller.remote.test;

import java.rmi.RemoteException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import junit.framework.TestCase;

import ddproto1.controller.constants.IErrorCodes;
import ddproto1.controller.exception.ServerRequestException;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;


public abstract class MultiDeathTest extends TestCase{
    
    private static final int SHUTDOWN_TIMEOUT = 30000;
    private static final int STREAM_PROCESSING_TIMEOUT = 5000;
    private static final int SHUTDOWN_POLLING_INTERVAL = 2000;
    
    private static final Logger logger = Logger.getLogger(MultiDeathTest.class);
    
    private volatile int pDead;
    protected volatile int nprocs;
    private ControlClientExt globalAgent;
    private IProcessServer pServerImpl;
    
    public void setUp(){
        pDead = 0;
    }
    
    /** 
     * Light test for the process server notification mechanisms. 
     * 
     * @throws Exception
     */
    public void testNotificationMechanismsLightly()
        throws Exception
    {
        LaunchParametersDTO[] lps = new LaunchParametersDTO[nprocs];

        for (int i = 0; i < nprocs; i++)
            lps[i] = TestUtils.crankDefaultLaunch(i, (i + 1) * 500, i);
        try {
            for (LaunchParametersDTO lp : lps) {
                IRemoteProcess rpprx = castToRemoteProcess(getPServerImpl()
                        .launch(lp));
                System.out.println("Launched process with handle: "
                        + rpprx.getHandle());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Wait for spontaneous death
        pollProcessesDeath();

        assertTrue(getGlobalAgent().isDone());

        for (Object rp : getPServerImpl().getProcessList()) {
            IRemoteProcess rpprx = castToRemoteProcess(rp);
            assertTrue(!rpprx.isAlive());
            rpprx.dispose();
        }
        
        pollProcessesDeath();

        assertTrue(getPServerImpl().getProcessList().size() == 0);
    }
    
    /**
     * Test for per-process shutdown requests. 
     * 
     * @throws Exception
     */
    public void testPerProcessKill()
        throws Exception
    {
        startEthernalProcesses(true);
        
        for(Object rp : getPServerImpl().getProcessList()){
            IRemoteProcess rprx = castToRemoteProcess(rp);
            assertTrue(rprx.isAlive());
            rprx.dispose();
        }
        
        pollProcessesDeath();

        assertTrue(getGlobalAgent().isDone());
        assertTrue(getPServerImpl().getProcessList().size() == 0);
    }
    
    /** 
     * Tests full shutdown. 
     */
    public void testServerKillWithInfiniteTimeout() throws Exception{
        startEthernalProcesses(true);
        
        Thread.sleep(STREAM_PROCESSING_TIMEOUT);
        
        /** Completely synchronous shut down. Method should only return
         * after all notifications have been processed, so we shouldn't
         * have to poll for death.
         */ 
        getPServerImpl().shutdownServer(true, 0);
        assertEquals(nprocs, pDead);
        assertTrue(getGlobalAgent().isDone());
    }
    
    /** 
     * Tests full shutdown. 
     */
    public void testAsynchronousServerKill() throws Exception{
        startEthernalProcesses(true);
        getPServerImpl().shutdownServer(true, -1);
        pollProcessesDeath();
        assertTrue(getGlobalAgent().isDone());
    }
    
    /** 
     * Tests full shutdown. 
     */
    public void testServerKillWithUnreasonableTimeout() throws Exception{
        startEthernalProcesses(true);
        try{
            getPServerImpl().shutdownServer(true, 1);
            fail("Server did not timeout.");
        }catch(RemoteException ex){
            assertTrue(ex.getCause() instanceof ServerRequestException);
            ServerRequestException sre = (ServerRequestException) ex.getCause();
            assertTrue(sre.getDetail() == IErrorCodes.TIMEOUT);
        }
    }
    
    public void testServerKillWithReasonableTimeout() throws Exception{
        startEthernalProcesses(true);
        try{
            getPServerImpl().shutdownServer(true, SHUTDOWN_TIMEOUT);
        }catch(RemoteException ex){
            logger.error(ex);
            if(ex.getCause() instanceof ServerRequestException){
                ServerRequestException sre = (ServerRequestException) ex.getCause();
                if(sre.getDetail() == IErrorCodes.TIMEOUT)
                    fail("Server timed out.");
            }
            fail();
        }
        assertEquals(nprocs, pDead);
        assertTrue(getGlobalAgent().isDone());
    }

    
    private void startEthernalProcesses(boolean waitStreamProcessingTimeout)
        throws Exception
    {
        LaunchParametersDTO [] lps = new LaunchParametersDTO[nprocs];
        
        for(int i = 0; i < nprocs; i++)
            lps[i] = TestUtils.crankDefaultLaunch(i, 20000000, i);
        
        for(LaunchParametersDTO lp : lps ){
            getPServerImpl().launch(lp);
        }
        
        if(waitStreamProcessingTimeout){
            synchronized(this){
                wait(STREAM_PROCESSING_TIMEOUT);
            }
        }
    }
    
    private void pollProcessesDeath() throws Exception{
        long startTime = System.currentTimeMillis();
        
        while(pDead < nprocs){
            synchronized(this){
                wait(SHUTDOWN_POLLING_INTERVAL);
            }
            if(System.currentTimeMillis() - startTime >= SHUTDOWN_TIMEOUT)
                fail("Test timeouted.");
        }
    }
    
    protected synchronized void notifyDeath(){
        pDead++;
        System.out.println("Dead processes: " + pDead);
        this.notifyAll();
    }

    /**
     * @return Returns the pServerImpl.
     */
    protected synchronized IProcessServer getPServerImpl() {
        return pServerImpl;
    }

    /**
     * @param serverImpl The pServerImpl to set.
     */
    protected synchronized void setPServerImpl(IProcessServer serverImpl) {
        pServerImpl = serverImpl;
    }

    protected synchronized void setGlobalAgent(ControlClientExt ccext){
        this.globalAgent = ccext;
    }

    protected synchronized ControlClientExt getGlobalAgent(){
        return this.globalAgent;
    }
    
    protected abstract IRemoteProcess castToRemoteProcess(Object prx);
}
