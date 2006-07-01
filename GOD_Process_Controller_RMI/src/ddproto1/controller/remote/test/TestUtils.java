/*
 * Created on Jun 18, 2006
 * 
 * file: TestUtils.java
 */
package ddproto1.controller.remote.test;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import ddproto1.controller.interfaces.LaunchParametersDTO;

public class TestUtils {
    
    private static final int DEFAULT_FLUSH_TIMEOUT = 500;
    private static final int DEFAULT_KEEPALIVE_INTERVAL = 400;
    private static final int MAX_BUFFERSIZE = 200;
    
    private static final AtomicInteger handles = new AtomicInteger(0);
    
    public static final String [] defaultLaunchString = { 
        "java", "-cp", null,
        "ddproto1.controller.remote.test.DelayAndDieApp"};
    
    private static Properties props; 
    
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
            props.load(TestUtils.class.getClassLoader().getResourceAsStream("test.properties"));
        }
        
        return props.getProperty(key);
    }
    
}
