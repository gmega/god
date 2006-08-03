package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;

public class SocketListenWrapper extends AbstractConnectorWrapper {

    private static Map<String, String> attributes = new HashMap<String, String>();

    static{
        attributes.put("listening-interface-ip", "localAddress");
        attributes.put("jdwp-port", "port");
        attributes.put("connection-timeout", "timeout");
        attributes.put("connector-type", null);
    }
	
	@Override
	protected String translate(String name) {
		return attributes.get(name);
	}
	
	protected boolean isValid(String name){ 
		return attributes.containsKey(name);
	}
	
	protected VirtualMachine doConnect(Connector c, Map<String, ? extends Connector.Argument> args)
		throws IllegalConnectorArgumentsException, IOException
	{
		ListeningConnector lConn = (ListeningConnector)c;
		try{
			return lConn.accept(args);
		}finally{
			lConn.stopListening(args);
		}
	}
	
	protected void doPrepare(Connector c, Map<String, ? extends Connector.Argument> args)
			throws IllegalConnectorArgumentsException, IOException{
		((ListeningConnector)c).startListening(args);
	}
}
