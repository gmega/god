/*
 * Created on Jun 18, 2006
 * 
 * file: TestUtils.java
 */
package ddproto1.controller.remote.test;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.rmi.PortableRemoteObject;

import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import ddproto1.remote.controller.MainServer;

public class TestUtils {
    
    private static final int DEFAULT_FLUSH_TIMEOUT = 200;
    private static final int DEFAULT_KEEPALIVE_INTERVAL = 400;
    private static final int MAX_BUFFERSIZE = 200;
    
    private static final AtomicInteger handles = new AtomicInteger(0);
    
    public static final String IOTESTAPP_CLASSPATH = "main.plugin.classes";
    public static final String LOG4J_CLASSPATH = "log4j.runtime.classpath";
    public static final String CONTROL_SERVER_CLASSPATH = "control.server.classpath";
    public static final String IO_TEST_TEXT_PATH = "io.test.text.path";
    
    public static final String IO_APP_MAINCLASS = "ddproto1.launcher.procserver.test.testapp.TestApplication";
    
    
    
    public static final String [] defaultLaunchString = { 
        "java", "-cp", null,
        "ddproto1.controller.remote.test.DelayAndDieApp"};
    
    private static Properties props; 
    
    private static final String CONTROLLER_REGISTRY_HOST = "localhost";
    private static final String CONTROLLER_REGISTRY_PORT = "3000";
    private static final String CONTROLLER_OBJNAME = "ControlClient";
    private static final String PROTOCOL = "tcp";
    private static final String PROCSERVER_ID = "testServer";
    private static Registry registry;

    public static void publishControlClient(IControlClient cc)
        throws Exception
    {
        PortableRemoteObject.exportObject(cc);
        IControlClient cClient = 
            (IControlClient)PortableRemoteObject.narrow(
                    PortableRemoteObject.toStub(cc), IControlClient.class);
        getRegistry().rebind(CONTROLLER_OBJNAME, cClient);
    }
    
    public static Process fireProcServerWithListener(IControlClient cc)
        throws Exception
    {
        publishControlClient(cc);
        // Now launch the process server. 
        ArrayList <String> al = new ArrayList<String>();
        al.add("java");
        al.add("-cp");
        al.add(TestUtils.getProperty(CONTROL_SERVER_CLASSPATH) + ":" 
                + TestUtils.getProperty(LOG4J_CLASSPATH));
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
        /** The server ID. */
        al.add(makeAttribute(ProcessServerConstants.PROCSERVER_IDENTIFIER, 
                PROCSERVER_ID));
        /** And tells it that it should start a new registry. */
        al.add(makeAttribute(ProcessServerConstants.LR_INSTANTIATION_POLICY, 
                ProcessServerConstants.SHOULD_START_NEW));
        
        
        String [] launchdata = new String[al.size()];
        launchdata = al.toArray(launchdata);
        
        return Runtime.getRuntime().exec(launchdata);
    }
    
    
    protected static synchronized Registry getRegistry()
        throws RemoteException
    {
        if(registry == null){
            registry = LocateRegistry.createRegistry(
                    Integer.parseInt(CONTROLLER_REGISTRY_PORT));
        }
        
        return registry;
    }
    
    private static String makeAttribute(String key, String val){
        return "--" + key + ProcessServerConstants.PARAM_SEPARATOR_CHAR + val;
    }
    
    public static LaunchParametersDTO crankDefaultLaunch(int id, int delay, int returnVal){
        
        try{
            if(defaultLaunchString[2] == null)
                defaultLaunchString[2] = getProperty("control.server.classpath");
        }catch(IOException ex){
            throw new RuntimeException(ex);
        }
        
        LaunchParametersDTO lPars = new LaunchParametersDTO(
                genCommandLine(defaultLaunchString, id, delay, returnVal),
                new LaunchParametersDTO.EnvironmentVariable [] { },
                DEFAULT_KEEPALIVE_INTERVAL,
                MAX_BUFFERSIZE,
                DEFAULT_FLUSH_TIMEOUT,
                handles.getAndIncrement());

        
        return lPars;
    }
    
    public static void resetHandles(){
    		handles.set(0);
    }
    
    public static String [] genCommandLine(String[] template, int id, int delay, int returnVal){
        String [] cmdLine = new String[template.length + 3];
        System.arraycopy(template, 0, cmdLine, 0, template.length);
        cmdLine[template.length] = Integer.toString(id);
        cmdLine[template.length + 1] = Integer.toString(delay);
        cmdLine[template.length + 2] = Integer.toString(returnVal);
        
        return cmdLine;
    }
    
    public synchronized static String getProperty(String key)
        throws IOException 
    {
        if(props == null){
            props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("test.properties"));
        }
        
        String propVal = props.getProperty(key);
        if(propVal == null) throw new RuntimeException("Property doesn't exist.");
        return propVal;
    }
    
}
