/*
 * Created on Jun 16, 2006
 * 
 * file: MainServer.java
 */
package ddproto1.remote.controller;

import java.io.IOException;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

import javax.rmi.PortableRemoteObject;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ddproto1.controller.constants.IErrorCodes;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IControlClient;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.remote.impl.RemoteProcessServerImpl;

public class MainServer implements ProcessServerConstants, IErrorCodes {

    private static Logger logger = null;
 
    public static void main(String[] args) {
        try {
            System.out.println("GOD Process Server V 0.1 alpha");
            Map<String, String> parameterMap = parseParameters(args);
            configureLogging(parameterMap.get(LOG4JCONFIG));
            logger = Logger.getLogger(MainServer.class);

            logger.info("Server is now being started...");
            /** We now must resolve the remote object. But first, we have
             * to resolve the remote registry.
             */
            Registry remoteRegistry =
                getRMIRegistry(SHOULD_USE_EXISTING,
                        parameterMap.get(CONTROLLER_REGISTRY_ADDRESS),
                        parameterMap.get(CONTROLLER_REGISTRY_PORT));
            
            IControlClient cClient = (IControlClient) PortableRemoteObject
                    .narrow(
                            remoteRegistry.lookup(parameterMap.get(CONTROLLER_REGISTRY_PATH)),
                            IControlClient.class);

            RemoteProcessServerImpl pSImpl = new RemoteProcessServerImpl(
                    cClient);
            
            String cookie = parameterMap.get(PROCSERVER_IDENTIFIER);
            if(cookie == null) cookie = "";
            pSImpl.setCookie(cookie);

            IProcessServer ips = (IProcessServer) PortableRemoteObject.narrow(
                    pSImpl.getProxyAndActivate(), IProcessServer.class);

            /** We now call back the client to tell him we're alive. */
            logger.info("Notifying client of server startup.");
            cClient.notifyServerUp(ips);

            logger.info("Process server is online.");

        } catch (Exception ex) {
            if(logger != null) logger.error("Failed to start server.", ex);
            ex.printStackTrace();
            System.exit(STATUS_ERROR);
        }

    }

    private static Registry getRMIRegistry(String startOption, String host, String portS) {

        int port;
        try {
            port = Integer.parseInt(portS);
            if (port < 0) {
                logger.error("Port must be a non-negative integer.");
                return null;
            }
        } catch (NumberFormatException ex) {
            logger.error("Port must be an integer.");
            return null;
        }

        Registry registry = null;

        try {
            if (startOption.equals(SHOULD_START_NEW)) {
                registry = LocateRegistry.createRegistry(port);
            } else if (startOption.equals(SHOULD_USE_EXISTING)) {
                registry = (host == null)?
                        LocateRegistry.getRegistry(port):
                        LocateRegistry.getRegistry(host, port);
            } else {
                logger.error("Unrecognized option " + startOption);
            }
        } catch (Exception ex) {
            logger.error("Error while starting RMI registry.", ex);
        }

        return registry;
    }

    private static Map<String, String> parseParameters(String[] args) {

        Map<String, String> attributes = new HashMap<String, String>();

        for (String arg : args) {

            String key, val;

            if (arg.startsWith("--")) {
                arg = arg.substring(2);
                String[] splitArg = arg.split(PARAM_SEPARATOR_CHAR);
                if (splitArg.length != 2) {
                    System.err.println("Invalid parameter syntax - " + arg);
                    continue;
                }
                key = splitArg[0];
                val = splitArg[1];
            } else {
                continue;
            }

            if (key.equals(CONTROLLER_REGISTRY_ADDRESS) 
            			|| key.equals(CONTROLLER_REGISTRY_PORT)
                    || key.equals(LOG4JCONFIG)
                    || key.equals(TRANSPORT_PROTOCOL)
                    || key.equals(CONTROLLER_REGISTRY_PATH)
                    || key.equals(LR_INSTANTIATION_POLICY)
                    || key.equals(PROCSERVER_IDENTIFIER)) {
                attributes.put(key, val);
            } else {
                System.err.println("Unrecognized parameter " + arg);
            }
        }

        if (!attributes.containsKey(CONTROLLER_REGISTRY_ADDRESS)
                || !attributes.containsKey(CONTROLLER_REGISTRY_PORT)
                || !attributes.containsKey(TRANSPORT_PROTOCOL)
                || !attributes.containsKey(CONTROLLER_REGISTRY_PATH)
                || !attributes.containsKey(LR_INSTANTIATION_POLICY)
                || !attributes.containsKey(PROCSERVER_IDENTIFIER)) {
            System.err.println("Missing required attributes.");
            System.exit(1);
        }

        return attributes;
    }

    private static void configureLogging(String surl) throws IOException {
        if (surl != null) {
            try {
                PropertyConfigurator.configure(new URL(surl));
                return;
            } catch (Exception ex) {
                System.err
                        .println("Failed to read configuration file for logger."
                                + " Falling back to default configuration.");
                ex.printStackTrace();
            }
        }

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }
}
