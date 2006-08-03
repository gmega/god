package ddproto1.debugger.managing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;


import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.ConfigException;
import ddproto1.exception.InternalError;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;

public abstract class AbstractConnectorWrapper implements IConfigurable, IJDIConnector{
	
	private static final String EMPTY_VALUE = "";
	
    private Map<String,String> attributeCache = new HashMap<String, String>();
    
    private Connector conn;
	
    public VirtualMachine connect() throws IOException, IllegalConnectorArgumentsException, ConfigException {
        try{

        	if(conn == null) throw new IllegalStateException("Cannot connect before preparing.");
            Map <String, ? extends Connector.Argument> argMap = this.setConnectorArgs(conn);
            return this.doConnect(conn, argMap);
        }catch(AttributeAccessException ex){
            throw new ConfigException("Configuration error.", ex);
        }
    }
    
    protected Connector findConnector(String conn_name) {
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
    
    protected Map <String, ? extends Connector.Argument> setConnectorArgs(Connector conn) throws AttributeAccessException{
        
        Map <String, ? extends Connector.Argument>def = conn.defaultArguments();

        for (String key : attributeCache.keySet()) {
                        
            String tKey = this.translate(key);
            
            if(tKey == null) continue;
            
            if (!def.containsKey(tKey))
                throw new InternalError(" Illegal connector arguments.");

            Connector.Argument arg = (Connector.Argument) def.get(tKey);
            
            /** Hack: the configurator won't allow optional attributes 
             * AND the Eclipse implementation of Connector.Argument 
             * complains with empty strings. So we test for empty strings
             * and don't set them.
             */
//            String attributeValue = attributeCache.get(key);
//            if(attributeValue.equals(EMPTY_VALUE)){
            	/** This is yet another particularity of the Eclipse JDI
            	 * implementation. We must set the value to null, or the
            	 * connector will complain about the default value. 
            	 */
//            	def.put(tKey, null);
//            	continue;
//            }
            arg.setValue(attributeCache.get(key));
        }

        return def;
    }
    
    protected void setAttributeValue(String key, String value){
    	attributeCache.put(key, value);
    }
    
    protected String getAttributeValue(String key){
    	return attributeCache.get(key);
    }    
    
    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
        if(!isValid(key)) throw new IllegalAttributeException();
        String value = getAttributeValue(key);
        if(value == null) throw new UninitializedAttributeException();
        return value;
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(!isValid(key)) throw new IllegalAttributeException("Unknown attribute " + key);
        setAttributeValue(key, val);
    }
    
    public boolean isWritable() {
        return true;
    }
    
    public void prepare() throws IOException, IllegalConnectorArgumentsException, ConfigException{
        try{
            conn = this.findConnector(this.getAttribute("connector-type"));
            
            Map <String, ? extends Connector.Argument> argMap = this.setConnectorArgs(conn);
            
            this.doPrepare(conn, argMap);
        }catch(AttributeAccessException ex){
            throw new ConfigException("Configuration error.", ex);
        }
    }

    
    protected abstract String translate(String name);
    protected abstract boolean isValid(String key);
    protected abstract void doPrepare(Connector conn, Map<String, ? extends Connector.Argument> args) 
    	throws IOException, IllegalConnectorArgumentsException;
    protected abstract VirtualMachine doConnect(Connector conn, Map<String, ? extends Connector.Argument> args)
    	throws IOException, IllegalConnectorArgumentsException;
}
