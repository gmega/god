/*
 * Created on Aug 8, 2005
 * 
 * file: SocketAttachWrapper.java
 */
package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.Connector.Argument;

import ddproto1.configurator.IConfigurable;
import ddproto1.exception.AttributeAccessException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InternalError;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.util.Lookup;

public class SocketAttachWrapper implements IConfigurable{

    private static Map<String, String> attributes = new HashMap<String, String>();

    static{
        attributes.put("local-agent-address", "hostname");
        attributes.put("local-agent-port", "port");
        attributes.put("attaching-connector-type", null);
    }
    
    private Map<String,String> attributeCache = new HashMap<String, String>();
    
    public SocketAttachWrapper() { }
    
    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
        if(!attributes.containsKey(key)) throw new IllegalAttributeException();
        String value = attributeCache.get(key);
        if(value == null) throw new UninitializedAttributeException();
        return value;
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(!attributes.containsKey(key)) throw new IllegalAttributeException("Unknown attribute " + key);
        attributeCache.put(key, val);
    }

    public Set<String> getAttributeKeys() {
        return attributes.keySet();
    }

    public VirtualMachine attach() throws IOException, IllegalConnectorArgumentsException, ConfigException {
        try{
            AttachingConnector conn = (AttachingConnector) this
                    .findConnector(this
                            .getAttribute("attaching-connector-type"));
            
            Map <String, ? extends Connector.Argument> argMap = this.setConnectorArgs(conn);
            
            return conn.attach(argMap);
        }catch(AttributeAccessException ex){
            throw new ConfigException("Configuration error.", ex);
        }
    }
    
    private Connector findConnector(String conn_name) {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector)iter.next();
            if (connector.name().equals(conn_name)) {
                return connector;
            }
        }
        return null;
    }
    
    private Map <String, ? extends Connector.Argument> setConnectorArgs(Connector conn) throws AttributeAccessException{
        
        Map <String, ? extends Connector.Argument>def = conn.defaultArguments();

        for (String key : attributeCache.keySet()) {
                        
            String tKey = attributes.get(key);
            
            if (!def.containsKey(tKey))
                throw new InternalError(" Illegal connector arguments.");

            Connector.Argument arg = (Connector.Argument) def.get(tKey);
            arg.setValue(attributeCache.get(key));
        }

        return def;
    }
}
