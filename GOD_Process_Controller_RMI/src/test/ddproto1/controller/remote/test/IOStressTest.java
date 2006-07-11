/*
 * Created on Jul 10, 2006
 * 
 * file: LotsOfIOTest.java
 */
package ddproto1.controller.remote.test;

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import junit.framework.TestCase;

/**
 * This is not really a test. It's just a client you may use 
 * if you wish to launch the process server on a debugger.
 * 
 * As it currently is, this test is designed to work with the 
 * test application provided by the main plug-in (package 
 * ddproto1.debugger.managing.tests.testapp.TestApplication 
 * 
 * @author giuliano
 *
 */
public class IOStressTest extends TestCase{
    
    private static final Logger logger = Logger.getLogger(IOStressTest.class);
    private IProcessServer procServer;
    private final CyclicBarrier barrier = new CyclicBarrier(2);
    
    private class IOMatcher implements IControlClient{

        public void notifyProcessDeath(int pHandle) throws RemoteException {
            System.out.println("Dead");
            parkIntoBarrier();
        }

        public void receiveStringFromSTDOUT(int pHandle, String data) throws RemoteException {
            System.out.print(data);
        }

        public void receiveStringFromSTDERR(int pHandle, String data) throws RemoteException {
            System.err.print(data);
        }

        public void notifyServerUp(IProcessServer procServer) 
            throws RemoteException {
            setServer(procServer);
            parkIntoBarrier();
        }
        
        private void parkIntoBarrier(){
            try{
                barrier.await();
            }catch(Exception ex){
                logger.error("Barrier has been breached.", ex);
            }
        }
    }
    
    private synchronized void setServer(IProcessServer procServer){
        this.procServer = procServer;
    }
    
    private synchronized IProcessServer getServer() { return procServer; }
    
    public void testProcServerIOCapability()
        throws Exception
    {
        String [] cmd = {
                "java", "-cp", 
                TestUtils.getProperty(TestUtils.IOTESTAPP_CLASSPATH) + ":" +
                TestUtils.getProperty(TestUtils.CONTROL_SERVER_CLASSPATH) + ":" +
                TestUtils.getProperty(TestUtils.LOG4J_CLASSPATH) + ":" + 
                TestUtils.getProperty(TestUtils.IO_TEST_TEXT_PATH),
                TestUtils.IO_APP_MAINCLASS
        };
        
        LaunchParametersDTO ldto = new LaunchParametersDTO(cmd, new LaunchParametersDTO.EnvironmentVariable [] { }, 42);
                
        TestUtils.publishControlClient(new IOMatcher());
        barrier.await();
        
        IRemoteProcess irp = getServer().launch(ldto);
        irp.writeToSTDIN("giuliano needs beer\n");
        
        barrier.await();
    }

}
