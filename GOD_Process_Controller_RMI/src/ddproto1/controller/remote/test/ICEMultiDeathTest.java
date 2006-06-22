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
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import javax.rmi.PortableRemoteObject;

import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.remote.controller.MainServer;

public class ICEMultiDeathTest extends MultiDeathTest{
    
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
            synchronized(ICEMultiDeathTest.this){
                ICEMultiDeathTest.this.notifyAll();
            }
        }
    }
    
    private static final int NPROCS = 5;
    private static final String CONTROLLER_REGISTRY_HOST = "localhost";
    private static final String CONTROLLER_REGISTRY_PORT = "3000";
    private static final String CONTROLLER_OBJNAME = "ControlClient";
    
    private static final String PROCSERVER_REGISTRY_PORT = "3001";
    
    private static final String PROTOCOL = "tcp";
    
    private Process serverProcess;
    
    public void setUp(){
        
        super.setUp();
        nprocs = NPROCS;
        
        //System.setSecurityManager(new SecurityManager());
        
        try{
            //System.setProperty("java.rmi.server.ignoreStubClasses", "true");
            // Publishes our object to the adapter.
            DummyController dc = new DummyController(NPROCS);
            PortableRemoteObject.exportObject(dc);
            IControlClient cClient = 
                (IControlClient)PortableRemoteObject.narrow(
                        PortableRemoteObject.toStub(dc), IControlClient.class);
            
            Registry reg = LocateRegistry.createRegistry(
            		Integer.parseInt(CONTROLLER_REGISTRY_PORT));
            reg.bind(CONTROLLER_OBJNAME, cClient);

            // Now launch the process server. 
            ArrayList <String> al = new ArrayList<String>();
            al.add("java");
            al.add("-cp");
            al.add(TestUtils.getProperty("control.server.classpath") + ":" 
                    + TestUtils.getProperty("log4j.runtime.classpath"));
            al.add(MainServer.class.getName());
            
            /** Tells the remote process server: */
            /** Where in our RMI registry the published controller resides. */
            al.add(makeAttribute(ProcessServerConstants.CONTROLLER_REGISTRY_PATH, 
            		CONTROLLER_OBJNAME));
            /** The address of our registry */
            al.add(makeAttribute(ProcessServerConstants.CONTROLLER_REGISTRY_ADDRESS, 
            		CONTROLLER_REGISTRY_HOST));
            /** The port of our registry */
            al.add(makeAttribute(ProcessServerConstants.CONTROLLER_REGISTRY_PORT, 
            		CONTROLLER_REGISTRY_PORT));
            /** The transport protocol to be adopted. */
            al.add(makeAttribute(ProcessServerConstants.TRANSPORT_PROTOCOL, 
            		PROTOCOL));

            /** Tells the process server in which port to start its RMI registry. */
            al.add(makeAttribute(ProcessServerConstants.LOCAL_REGISTRY_PORT, 
            		PROCSERVER_REGISTRY_PORT));
            /** And tells it that it should start a new registry. */
            al.add(makeAttribute(ProcessServerConstants.LR_INSTANTIATION_POLICY, 
            		ProcessServerConstants.SHOULD_START_NEW));
            
            
            String [] launchdata = new String[al.size()];
            launchdata = al.toArray(launchdata);
            
            setProcess(Runtime.getRuntime().exec(launchdata));
            
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
        }
    }
    
    protected void cleanup(){
        try{
            getPServerImpl().shutdownServer(true);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void tearDown(){
        cleanup();
    }

    private String makeAttribute(String key, String val){
        return "--" + key + ProcessServerConstants.PARAM_SEPARATOR_CHAR + val;
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
