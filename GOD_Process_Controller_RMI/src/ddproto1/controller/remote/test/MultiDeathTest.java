/*
 * Created on Jun 18, 2006
 * 
 * file: MultiDeathTest.java
 */
package ddproto1.controller.remote.test;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;

import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;


public abstract class MultiDeathTest extends TestCase{
    
    private volatile int pDead;
    protected volatile int nprocs;
    private ControlClientExt globalAgent;
    private IProcessServer pServerImpl;
   
    public void setUp(){
        BasicConfigurator.configure();
    }
    
    public void testNotificationsWith()
        throws Exception
    {
        pDead = 0;
        
        LaunchParametersDTO [] lps = new LaunchParametersDTO[nprocs];
        
        for(int i = 0; i < nprocs; i++)
            lps[i] = TestUtils.crankDefaultLaunch(i, (i+1)*500, i);
        
        for(LaunchParametersDTO lp : lps ){
            IRemoteProcess rpprx = 
                castToRemoteProcess(getPServerImpl().launch(lp));
            System.out.println("Launched process with handle: " + rpprx.getHandle());
        }
        
        long startTime = System.currentTimeMillis();
        
        synchronized(this){
            while(pDead < nprocs){
                wait();
                if(System.currentTimeMillis() - startTime >= 30000)
                    fail("Test timeouted.");
            }
            
            /** Wait to receive all strings. */
            wait(5000);
        }
        
        assertTrue(getGlobalAgent().isDone());
        
        for(Object rp : getPServerImpl().getProcessList()){
            IRemoteProcess rpprx = castToRemoteProcess(rp);
            assertTrue(!rpprx.isAlive());       
            rpprx.dispose();
        }
        
        assertTrue(getPServerImpl().getProcessList().size() == 0);
    }
    
    public void testForcedKill()
        throws Exception
    {
        pDead = 0;
        
        LaunchParametersDTO [] lps = new LaunchParametersDTO[nprocs];
        
        for(int i = 0; i < nprocs; i++)
            lps[i] = TestUtils.crankDefaultLaunch(i, 20000000, i);
        
        for(LaunchParametersDTO lp : lps ){
            getPServerImpl().launch(lp);
        }
        
        long startTime = System.currentTimeMillis();
        
        List listCopy = 
            getPServerImpl().getProcessList();
        
        /** Wait for strings to be sent before killing processes. */
        synchronized(this){
            wait(5000);
        }
        
        for(Object rp : getPServerImpl().getProcessList()){
            IRemoteProcess rprx = castToRemoteProcess(rp);
            assertTrue(rprx.isAlive());
            rprx.dispose();
        }
        
        synchronized(this){
            while(pDead < nprocs){
                wait();
                if(System.currentTimeMillis() - startTime >= 30000)
                    fail("Test timeouted.");
            }
            wait(5000);
        }
        
        assertTrue(getGlobalAgent().isDone());

// Doesn't make sense for this test. The call to 'dispose' destroys the remote objects.
//        for(Object rp : listCopy){
//            RemoteProcessPrx rprx = castToPrx(rp);
//            assertTrue(!rprx.isAlive());            
//        }
        
        assertTrue(getPServerImpl().getProcessList().size() == 0);
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
