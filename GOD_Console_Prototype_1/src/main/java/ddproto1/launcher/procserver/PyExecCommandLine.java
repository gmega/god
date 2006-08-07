/*
 * Created on 18/07/2006
 * 
 * file: PySSHExecutorCommandLine.java
 */
package ddproto1.launcher.procserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.ICommandLine;
import ddproto1.util.TestUtils;

public class PyExecCommandLine implements ICommandLine{
    
    private final Map<String, String> attributes =
        Collections.synchronizedMap(new HashMap<String, String>());
    
    private ICommandLine baseLine;
    
    public PyExecCommandLine(ICommandLine baseLine){
        this.baseLine = baseLine;
        attributes.put(IConfigurationConstants.ID_ATTRIB, null);
        attributes.put(IConfigurationConstants.GLOBAL_AGENT_ADDRESS, null);
        attributes.put(IConfigurationConstants.RMI_REGISTRY_PORT, null);
        attributes.put(ProcessServerConstants.LOG4JCONFIG, null);
        attributes.put(ProcessServerConstants.LR_INSTANTIATION_POLICY, null);
        attributes.put(ProcessServerConstants.TRANSPORT_PROTOCOL, null);
        attributes.put(IConfigurationConstants.CALLBACK_OBJECT_PATH, null);
    }
    
    public synchronized String[] renderCommandLine() throws AttributeAccessException {
        String [] cLine = baseLine.renderCommandLine();
        String [] extra = extractCommandSwitches();
        String [] augmented = new String[cLine.length + extra.length];
        System.arraycopy(cLine, 0, augmented, 0, cLine.length);
        System.arraycopy(extra, 0, augmented, cLine.length, extra.length);
        return augmented;
    }
    
    private String [] extractCommandSwitches()
        throws AttributeAccessException
    {
        List<String> atts = new ArrayList<String>();
        atts.add(makeAttribute(
                ProcessServerConstants.CONTROLLER_REGISTRY_ADDRESS,
                getAttribute(IConfigurationConstants.GLOBAL_AGENT_ADDRESS)));
        atts.add(makeAttribute(
                ProcessServerConstants.CONTROLLER_REGISTRY_PATH,
                getAttribute(IConfigurationConstants.CALLBACK_OBJECT_PATH)));
        atts.add(makeAttribute(
                ProcessServerConstants.CONTROLLER_REGISTRY_PORT,
                getAttribute(IConfigurationConstants.RMI_REGISTRY_PORT)));
        atts.add(makeAttribute(
                ProcessServerConstants.LOG4JCONFIG,
                getAttribute(ProcessServerConstants.LOG4JCONFIG)));
        atts.add(makeAttribute(
                ProcessServerConstants.TRANSPORT_PROTOCOL,
                ProcessServerConstants.TCP));
        atts.add(makeAttribute(
                ProcessServerConstants.LR_INSTANTIATION_POLICY,
                ProcessServerConstants.SHOULD_START_NEW));
        atts.add(makeAttribute(ProcessServerConstants.PROCSERVER_IDENTIFIER,
                getAttribute(IConfigurationConstants.ID_ATTRIB)));
        String [] extra = new String[atts.size()];
        return atts.toArray(extra);
    }
    
    private String makeAttribute(String key, String value){
        return "--" + key + "=" + value;
    }

    public synchronized void addApplicationParameter(String parameter) { 
        baseLine.addApplicationParameter(parameter);
    }

    public synchronized String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
        if(attributes.containsKey(key)){
            String val = attributes.get(key);
            if(val == null) throw new UninitializedAttributeException();
            return val;
        }
        return baseLine.getAttribute(key);
    }

    public synchronized void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(attributes.containsKey(key)) attributes.put(key, val);
        else baseLine.setAttribute(key, val);
    }

    public synchronized boolean isWritable() {
        return true;
    }

}
