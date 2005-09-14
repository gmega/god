/*
 * Created on Sep 13, 2005
 * 
 * file: CORBAHookInitWrapper.java
 */
package ddproto1.localagent.instrumentation;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ObjectType;
import org.apache.log4j.Logger;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.CommException;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;

public class CORBAHookInitWrapper implements IClassLoadingHook {
    
    private CORBAHook surrogate;
    private boolean disabled = false;
    private static final Logger logger = Logger.getLogger(IClassLoadingHook.class);
    
    public JavaClass modifyClass(JavaClass jc) {
        try{
            if(surrogate == null && !disabled) init();
        }catch(Exception ex){
            logger.error("Failed to initialize the CORBA instrumentation hook. " +
                    "No stubs or skeletons will be instrumented.", ex);
            disabled = true;
        }
        return disabled?jc:surrogate.modifyClass(jc);
    }
    
    private void init() 
        throws IOException, UnknownHostException, CommException
    {
        /* Creates the proxy to the global agent */
        GlobalAgentFactory gaFactory = GlobalAgentFactory.getInstance();
        IGlobalAgent globalAgent = gaFactory.resolveReference();
        
        /** Acquires the stub and skeleton list from the global agent. */
        surrogate = new CORBAHook(
                this.grabList(IConfigurationConstants.STUB_LIST, globalAgent), 
                this.grabList(IConfigurationConstants.SKELETON_LIST, globalAgent));
    }
    
    private ObjectType[] grabList(String listName, IGlobalAgent globalAgent)
            throws IOException, CommException {
        String list = globalAgent.getAttribute(listName);
        String[] types = list
                .split(IConfigurationConstants.LIST_SEPARATOR_CHAR);
        ObjectType[] obtypes = new ObjectType[types.length];
        for (int i = 0; i < types.length; i++)
            obtypes[i] = new ObjectType(types[i]);

        return obtypes;
    }

}
