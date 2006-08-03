/*
 * Created on Aug 8, 2005
 * 
 * file: SocketAttachWrapper.java
 */
package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class SocketAttachWrapper extends AbstractConnectorWrapper{

    private static Map<String, String> attributes = new HashMap<String, String>();

    static{
        attributes.put("local-agent-address", "hostname");
        attributes.put("jdwp-port", "port");
        attributes.put("connector-type", null);
    }
    
    public SocketAttachWrapper() { }
    
    protected boolean isValid(String key){
    	return attributes.containsKey(key);
    }
    
    protected String translate(String key){
    	return attributes.get(key);
    }
    
    protected VirtualMachine doConnect(Connector conn, Map<String, ? extends Connector.Argument> args) 
    	throws IOException, IllegalConnectorArgumentsException{
    	return ((AttachingConnector)conn).attach(args);
    }
    
    protected void doPrepare(Connector conn, Map<String, ? extends Connector.Argument> args){ }
}
