/*
 * Created on 19/07/2006
 * 
 * file: LocalAgentCommandLine.java
 */
package ddproto1.launcher.procserver;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.AttributeStore;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.ICommandLine;
import ddproto1.launcher.IJVMCommandLine;
import ddproto1.util.Lookup;

public class LocalAgentCommandLine implements ICommandLine, IConfigurationConstants {

    private final AttributeStore attributes = new AttributeStore();
    private IJVMCommandLine jvmCLine;
     
    public LocalAgentCommandLine(){
        attributes.declareAttribute(GUID_ATTRIBUTE);
        attributes.declareAttribute(LOG4J_CONFIG_URL);
        attributes.declareAttribute(NAME_ATTRIB);
        attributes.declareAttribute(GLOBAL_AGENT_ADDRESS);
        attributes.declareAttribute(CDWP_PORT);
        attributes.declareAttribute(CONN_POOL_SIZE);
    }
    
    public String[] renderCommandLine() throws AttributeAccessException {
    	try{
    		IJVMCommandLine commLine = getCommandLine();
    		commLine.addVMParameter("-D"
    				+ LOCAL_AGENT_GID_OPT + "="
    				+ getGID());
            
            commLine.addVMParameter("-D" + 
                    LOCAL_AGENT_GA_ADDRESS_OPT + "="
                    + getAttribute(GLOBAL_AGENT_ADDRESS) + ":" +
                    getAttribute(CDWP_PORT));
            
            commLine.addVMParameter("-D" +
                    LOCAL_AGENT_CONNPOOL_OPT + "=" + 
                    getAttribute(CONN_POOL_SIZE));
            
            String log4jconf = getAttribute(LOG4J_CONFIG_URL);
            if(!log4jconf.equals(AUTO)){
                commLine.addVMParameter("-D"
                        + LOCAL_AGENT_LOG4J_OPT + "="
                        + log4jconf);
            }
            
    		return commLine.renderCommandLine();
    	}catch(Exception ex){
    		throw new IllegalAttributeException("Error while rendering " +
    				"command line. ", ex);
    	}
    }

    public void addApplicationParameter(String parameter) 
    {
    	try{
    		getCommandLine().addApplicationParameter(parameter);
    	}catch(Exception ex){
    		throw new NestedRuntimeException("Cannot add parameter.", ex);
    	}
    }
    
    private String getGID()
    	throws AttributeAccessException
    {
    	String parentName = getAttribute(NAME_ATTRIB);
    	String attr = getAttribute(GUID_ATTRIBUTE);
    	if(attr.equals(AUTO)){
    		IObjectSpec nodeSpec = GODBasePlugin.getDefault()
					.getConfigurationManager().getNodelist().getSpec(parentName);
    		if(nodeSpec == null)
    			throw new IllegalAttributeException("Node specification " + parentName);
			attr = GODBasePlugin.getDefault().debuggeeGUIDManager()
					.currentlyLeasedGUID(nodeSpec).toString();
    	}
    	
    	return attr;
    }
    
    private IJVMCommandLine getCommandLine()
    	throws NoSuchSymbolException, AmbiguousSymbolException, IncarnationException
    {
    	if(jvmCLine == null){
    		IServiceLocator sLocator = (IServiceLocator)
        		Lookup.serviceRegistry().locate(IConfigurationConstants.SERVICE_LOCATOR);
    	
    		IObjectSpec _this = sLocator.getMetaobject(this);
    		IObjectSpec cLine = _this.getChildSupporting(ICommandLine.class);
    	
    		jvmCLine = (IJVMCommandLine)sLocator.incarnate(cLine);
    	}
    	return jvmCLine;
    }

    public String getAttribute(String key) throws IllegalAttributeException,
            UninitializedAttributeException {
        return attributes.getAttribute(key);
    }

    public void setAttribute(String key, String val)
            throws IllegalAttributeException, InvalidAttributeValueException {
        attributes.setAttribute(key, val);
    }

    public boolean isWritable() { return true; }

}
