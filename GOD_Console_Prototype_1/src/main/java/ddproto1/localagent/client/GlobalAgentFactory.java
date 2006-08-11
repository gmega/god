/*
 * Created on Sep 20, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: GlobalAgentFactory.java
 */

package ddproto1.localagent.client;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.localagent.client.PooledConnectionManager.WaitUntilAvailablePolicy;


/**
 * @author giuliano
 *
 */
public class GlobalAgentFactory {
    //private static final Logger logger = Logger.getLogger(GlobalAgentFactory.class);
    
    private static final GlobalAgentFactory instance = new GlobalAgentFactory();
    
    private static final int DEFAULT_POOLSIZE = 5;
    
    public synchronized static GlobalAgentFactory getInstance(){
        return instance;
    }
    
    private GlobalAgentFactory() { }
    
    private IGlobalAgent the_agent = null;
    
    public IGlobalAgent resolveReference() 
    	throws UnknownHostException, IOException
    {
        
        if(the_agent != null)
            return the_agent;
        
        String address = System.getProperty(IConfigurationConstants.LOCAL_AGENT_GA_ADDRESS_OPT);
        String giddy   = System.getProperty(IConfigurationConstants.LOCAL_AGENT_GID_OPT);
        String params  = System.getenv("agent.local.parameters");
        
        int poolsize = -1;
        
        if(params != null)
            poolsize = Integer.parseInt(params);
        
        byte localgid = Byte.parseByte(giddy);
        
        /* Looks up the proxy map */
        String hostSpec[] = address.split(":");
        
        if(hostSpec.length != 2)
            throw new IllegalArgumentException("Malformed address string - must be of the form IP-address:port");

        IConnectionManager icmgr = new PooledConnectionManager(
                (poolsize == -1)?DEFAULT_POOLSIZE:poolsize, 
                        hostSpec[0], Integer.parseInt(hostSpec[1]),
                        new WaitUntilAvailablePolicy());
        
        the_agent = new GlobalAgentProxyImpl(icmgr, localgid);
                        
        return the_agent;
    }
}
