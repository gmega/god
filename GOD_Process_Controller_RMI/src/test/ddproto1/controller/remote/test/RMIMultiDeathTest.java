/*
 * Created on Jun 19, 2006
 * 
 * file: ICEMultiDeathTest.java
 */
package ddproto1.controller.remote.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import javax.rmi.PortableRemoteObject;

import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.remote.controller.MainServer;

public class RMIMultiDeathTest extends MultiDeathTest{
    
    private class DummyController implements IControlClient{
        
        private ControlClientOps ops;
        
        public DummyController(int nProcs){
            this.setOps(new ControlClientOps(nProcs));
        }
        
        protected synchronized void setOps(ControlClientOps ops){
            this.ops = ops;
        }
        
        protected synchronized ControlClientOps getOps(){
            return ops;
        }

        public void notifyProcessDeath(int pHandle) {
            getOps().notifyProcessDeath(pHandle);
            System.out.println("Got notifyProcessDeath.");
            notifyDeath();
        }

        public void receiveStringFromSTDOUT(int pHandle, String data) {
            getOps().receiveStringFromSTDOUT(pHandle, data);
        }

        public void receiveStringFromSTDERR(int pHandle, String data) {
            getOps().receiveStringFromSTDERR(pHandle, data);
        }
        
        public boolean isDone(){
            return getOps().isDone();
        }

        public void notifyServerUp(IProcessServer procServer) {
            setPServerImpl(procServer);
            synchronized(RMIMultiDeathTest.this){
                RMIMultiDeathTest.this.notifyAll();
            }
        }
    }
    
    private static final int NPROCS = 5;
    
    private Process serverProcess;
    
    private static Registry registry;
    
    public void setUp(){
        
        super.setUp();
        nprocs = NPROCS;
        
        //System.setSecurityManager(new SecurityManager());
        
        DummyController dc = null;
        try{
        	TestUtils.resetHandles();
            //System.setProperty("java.rmi.server.ignoreStubClasses", "true");
            // Publishes our object to the adapter.
            dc = new DummyController(NPROCS);
            setProcess(TestUtils.fireProcServerWithListener(dc));
            
            new Thread(new Runnable(){
                public void run() {
                    BufferedReader reader = new BufferedReader( 
                        new InputStreamReader(getProcess().getErrorStream()));
                    String line;
                    try{
                        while((line = reader.readLine()) != null)
                            System.out.println("Process Server: " + line);
                    }catch(IOException ex){
                        System.out.println("IOException. Stopping.");
                    }
                }
            }).start();
            
            new Thread(new Runnable(){
                public void run() {
                    BufferedReader reader = new BufferedReader( 
                        new InputStreamReader(getProcess().getInputStream()));
                    String line;
                    try{
                        while((line = reader.readLine()) != null)
                            System.out.println("Process Server: " + line);
                    }catch(IOException ex){
                        System.out.println("IOException. Stopping.");
                    }
                }
            }).start();
            
            System.out.println("setUp waiting for server callback...");
            
            /** Waits until the server calls back. */
            synchronized(this){
                while(getPServerImpl() == null)
                    this.wait();
            }
            
            System.out.println("Server has called back.");
            
            setGlobalAgent(adaptCCToPrxExt(dc));
            
        }catch(Exception ex){
            ex.printStackTrace();
            cleanup();
            fail();
        }
    }
    
    protected void cleanup(){
        try{
            getPServerImpl().shutdownServer(true);
            // Have to stall because of the hack we must do
            // to prevent RMI server shutdowns from producing
            // spurious exceptions. If we don't stall, the setUp
            // for the next test run will clash when it attempts
            // to start the remote server in the same port as 
            // the one that's shutting down.
            Thread.sleep(4000);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void tearDown(){
        cleanup();
    }

    protected synchronized void setProcess(Process p){
        serverProcess = p;
    }
    
    protected synchronized Process getProcess(){
        return serverProcess;
    }

    @Override
    protected IRemoteProcess castToRemoteProcess(Object prx) {
        return (IRemoteProcess)PortableRemoteObject.narrow(prx, IRemoteProcess.class);
    }
    
    
    private ControlClientExt adaptCCToPrxExt(final DummyController dc) {
        return (ControlClientExt) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class [] { ControlClientExt.class }, new InvocationHandler(){
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if(method.getName().equals("isDone"))
                            return dc.isDone();
                        
                        throw new UnsupportedOperationException();
                    }            
        });
    }
}
