/*
 * Created on Jun 18, 2006
 * 
 * file: TestUtils.java
 */
package ddproto1.controller.remote.test;

import java.io.IOException;
import java.util.Properties;

import ddproto1.controller.client.LaunchParameters;

public class TestUtils {
    
    private static final int DEFAULT_FLUSH_TIMEOUT = 500;
    private static final int DEFAULT_KEEPALIVE_INTERVAL = 400;
    private static final int MAX_BUFFERSIZE = 200;
    
    public static final String [] defaultLaunchString = { 
        "java", "-cp", null,
        "ddproto1.controller.remote.test.DelayAndDieApp"};
    
    private static Properties props; 
    
    public static LaunchParameters crankDefaultLaunch(int id, int delay, int returnVal){
        LaunchParameters lPars = new LaunchParameters();
        
        try{
            if(defaultLaunchString[2] == null)
                defaultLaunchString[2] = getProperty("control.server.classpath");
        }catch(IOException ex){
            throw new RuntimeException(ex);
        }
        
        lPars.flushTimeout = DEFAULT_FLUSH_TIMEOUT;
        lPars.maxUnflushedSize = MAX_BUFFERSIZE;
        lPars.pollInterval = DEFAULT_KEEPALIVE_INTERVAL;
        lPars.commandLine = genCommandLine(defaultLaunchString, id, delay, returnVal);
        
        return lPars;
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
