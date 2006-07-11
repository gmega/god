/*
 * Created on Jun 19, 2006
 * 
 * file: ProcessServerConstants.java
 */
package ddproto1.controller.constants;

public interface ProcessServerConstants {
    public static final String LOG4JCONFIG = "log4j-config-url";
    
    public static final String CONTROLLER_REGISTRY_ADDRESS = "controller-address";
    public static final String CONTROLLER_REGISTRY_PATH = "controller-path";
    public static final String CONTROLLER_REGISTRY_PORT = "listening-port";
    public static final String TRANSPORT_PROTOCOL = "transport-protocol";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    
    public static final String PROCSERVER_IDENTIFIER = "server-id"; 
    
    public static final String LR_INSTANTIATION_POLICY = "registry-instantiation-policy";
    public static final String SHOULD_START_NEW = "N";
    public static final String SHOULD_USE_EXISTING = "E";
        
    public static final String PARAM_SEPARATOR_CHAR = "=";
    
    public static final String PROC_SERVER_MAINCLASS = "ddproto1.remote.controller.MainServer";

}
