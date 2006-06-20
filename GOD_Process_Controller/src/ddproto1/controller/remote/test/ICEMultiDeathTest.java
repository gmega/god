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
import java.util.ArrayList;

import Ice.Communicator;
import Ice.Current;
import Ice.LocalException;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;
import Ice.Util;
import ddproto1.controller.client._ControlClientDisp;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.remote.ProcessServerPrx;
import ddproto1.controller.remote.ProcessServerPrxHelper;
import ddproto1.controller.remote.RemoteProcessPrx;
import ddproto1.controller.remote.RemoteProcessPrxHelper;
import ddproto1.remote.controller.MainServer;

public class ICEMultiDeathTest extends MultiDeathTest{
    
    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 1;
    
    private class DummyController extends _ControlClientDisp{
        
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

        public void notifyProcessDeath(int pHandle, Current __current) {
            getOps().notifyProcessDeath_async(null, pHandle);
            System.out.println("Got notifyProcessDeath.");
            notifyDeath();
        }

        public void receiveStringFromSTDIN(int pHandle, String data, Current __current) {
            getOps().receiveStringFromSTDIN_async(null, pHandle, data);
        }

        public void receiveStringFromSTDERR(int pHandle, String data, Current __current) {
            getOps().receiveStringFromSTDERR_async(null, pHandle, data);
        }
        
        public boolean isDone(){
            return getOps().isDone();
        }

        public void notifyServerUp(ProcessServerPrx procServer, Current __current) {
            setPServerImpl(procServer);
            synchronized(ICEMultiDeathTest.this){
                ICEMultiDeathTest.this.notifyAll();
            }
        }
    }
    
    private static final int NPROCS = 5;
    private static final String CONTROLLER_HOST = "localhost";
    private static final String CONTROLLER_PORT = "3000";
    private static final String CONTROLLER_OBJNAME = "ControlClient";
    private static final String CONTROLLER_ADAPTER = "ControlClientAdapter";
    private static final String PROCSERVER_PORT = "3001";
    private static final String PROCSERVER_HOST = "localhost";
    
    private static final String PROTOCOL = "tcp";
    
    private Process serverProcess;
    
    private Communicator ic;

    public void setUp(){
        
        super.setUp();
        nprocs = NPROCS;
        
        try{
            
            ic = Util.initialize(new String [] {});

            // Publishes our object to the adapter.
            
            ObjectAdapter adapter = 
                ic.createObjectAdapterWithEndpoints(CONTROLLER_ADAPTER, PROTOCOL + " -p " + CONTROLLER_PORT);
            DummyController dc = new DummyController(NPROCS);
            adapter.add(dc, Util.stringToIdentity(CONTROLLER_OBJNAME));
            
            adapter.activate();
            
            // Now launch the process server. 
            ArrayList <String> al = new ArrayList<String>();
            al.add("java");
            al.add("-cp");
            al.add(TestUtils.getProperty("control.server.classpath") + ":" 
                    + TestUtils.getProperty("ice.runtime.classpath") + ":"
                    + TestUtils.getProperty("log4j.runtime.classpath"));
            al.add(MainServer.class.getName());
            al.add(makeAttribute(ProcessServerConstants.CONTROLLER_ADDRESS, 
                    CONTROLLER_OBJNAME + ":" + 
                    PROTOCOL + 
                    " -h " + CONTROLLER_HOST + 
                    " -p " + CONTROLLER_PORT));
            al.add(makeAttribute(ProcessServerConstants.REQUEST_PORT, PROCSERVER_PORT));
            al.add(makeAttribute(ProcessServerConstants.TRANSPORT_PROTOCOL, PROTOCOL));
            
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
        if(ic != null){
            try{
                getPServerImpl().shutdownServer(true);
                ic.destroy();
            } catch (Exception ex) {
                System.err.println("Error shutting down communicator.");
                ex.printStackTrace();
            }
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
    protected RemoteProcessPrx castToPrx(Object prx) {
        return RemoteProcessPrxHelper.checkedCast((ObjectPrx)prx);
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
