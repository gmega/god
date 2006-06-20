/*
 * Created on Jun 16, 2006
 * 
 * file: MainServer.java
 */
package ddproto1.remote.controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ddproto1.controller.client.ControlClientPrx;
import ddproto1.controller.client.ControlClientPrxHelper;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.remote.ProcessServerPrx;
import ddproto1.controller.remote.ProcessServerPrxHelper;
import ddproto1.controller.remote.impl.ProcessServerImpl;

import Ice.Communicator;
import Ice.LocalException;
import Ice.ObjectAdapter;
import Ice.Util;

public class MainServer implements ProcessServerConstants{
    
    private static final String ADAPTER_NAME = "ProcessServerAdapter";
    private static Logger logger;
    
    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 1;
    
    public static void main(String [] args){
        int status = STATUS_OK;
        Communicator ic = null;
        
        try{
            System.out.println("GOD Process Server V 0.1 alpha");
            Map<String, String> parameterMap = parseParameters(args);
            configureLogging(parameterMap.get(LOG4JCONFIG));
            
            ic = Util.initialize(args);
            
            ControlClientPrx controlClient =
                ControlClientPrxHelper.checkedCast(
                        ic.stringToProxy(parameterMap.get(CONTROLLER_ADDRESS)));
            
            ObjectAdapter adapter = 
                ic.createObjectAdapterWithEndpoints(ADAPTER_NAME,
                        parameterMap.get(TRANSPORT_PROTOCOL) + " -p " + parameterMap.get(REQUEST_PORT));
            
            ProcessServerImpl pSImpl = 
                new ProcessServerImpl(controlClient, adapter, Util.stringToIdentity(OBJECT_NAME));
            ProcessServerPrx proxy = 
                ProcessServerPrxHelper.uncheckedCast(pSImpl.activateAndGetProxy());
            
            adapter.activate();
            
            /** We now call back the client to tell him we're alive. */
            logger.info("Notifying client of server startup.");
            controlClient.notifyServerUp(proxy);
            
            logger.info("Process server is online.");
            ic.waitForShutdown();
            
            logger.info("ICE communicator has been shut down.");
            
        }catch(LocalException lex){
            lex.printStackTrace(); // Can't rely on logger being set up.
            status = STATUS_ERROR;
        }catch(Exception ex){
            ex.printStackTrace();
            status = STATUS_ERROR;
        }
        
        if(ic != null){
            try{
                ic.destroy();
            } catch (Exception ex) {
                System.err.println("Error shutting down communicator.");
                ex.printStackTrace();
                status = STATUS_ERROR;
            }
        }
        
        System.exit(status);
    }
    
    private static Map<String, String> parseParameters(String [] args){
        
        Map <String, String> attributes =
            new HashMap<String, String>();
        
        for(String arg : args){
            
            String key, val;
            
            if(arg.startsWith("--")){
                arg = arg.substring(2);
                String [] splitArg = arg.split(PARAM_SEPARATOR_CHAR);
                if(splitArg.length != 2){
                    System.err.println("Invalid parameter syntax - " + arg);
                    continue;
                }
                key = splitArg[0];
                val = splitArg[1];
            }else{
                continue;
            }
            
            if(key.equals(CONTROLLER_ADDRESS) || 
                    key.equals(REQUEST_PORT)  || 
                    key.equals(LOG4JCONFIG) || 
                    key.endsWith(TRANSPORT_PROTOCOL)){
                attributes.put(key, val);
            }else{
                System.err.println("Unrecognized parameter " + arg);
            }
        }
        
        if(!attributes.containsKey(CONTROLLER_ADDRESS) ||
                !attributes.containsKey(REQUEST_PORT) ||
                !attributes.containsKey(TRANSPORT_PROTOCOL)){
            System.err.println("Missing required attributes.");
            System.exit(1);
        }
            
        
        return attributes;
    }
    
    private static void configureLogging(String surl) throws IOException{
        if(surl != null){
            try{
                PropertyConfigurator.configure(new URL(surl));
                return;
            }catch(Exception ex){
                System.err.println("Failed to read configuration file for logger." +
                        " Falling back to default configuration.");
                ex.printStackTrace();
            }
        }
        
        BasicConfigurator.configure();
        logger = Logger.getLogger(MainServer.class);
    }

}
