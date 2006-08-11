/*
 * Created on Apr 27, 2005
 * 
 * file: AttributeConstants.java
 */
package ddproto1.configurator.commons;

import java.io.File;

public interface IConfigurationConstants {
    // TODO: Move all this stuff to a configuration file */
    /** Attribute keys for the configuration verification mechanism */
    public static final String SPEC_ATTRIB              = "spec";
    public static final String TYPE_ATTRIB              = "type";
    public static final String NAME_ATTRIB 				= "name";
    public static final String CHILD_ATTRIB 			= "child";
    public static final String SPEC_EXT_ATTRIB 			= "spec-extension";
    public static final String OPTIONAL_ATTRIB			= "extension-attribute";
    public static final String PROPERTY_ATTRIB 			= "attribute";
    public static final String ID_ATTRIB 				= "id";
    public static final String ACTION_ATTRIBUTE 		= "action";
    public static final String ROOT_ATTRIB 				= "root-element";
    public static final String ELEMENT_ATTRIB 			= "element";
    public static final String MULTIPLICITY_ATTRIB 		= "multiplicity";
    public static final String INTENDED_INTF_ATTRIB 	= "intended-interfaces";
    public static final String DEFAULT_VALUE_ATTRIB 	= "default-value";
        
    /** These define the attributes that the XMLConfigurationParser
     * interprets as attribute assignments in the configuration files.
     */
    public static final String PARAM_ATTRIB 			= "param";
    public static final String PARAM_KEY_ATTRIB			= "key";
    public static final String PARAM_VAL_ATTRIB 		= "value";
    
    /** Client constants (attribute keys accessed by a multitude of 
     * other classes). */
    public static final String NODE_LIST 				= "node-list";
    public static final String NODE 					= "node";
    public static final String PORT 					= "cdwp-port";
    public static final String MAX_QUEUE_LENGTH 		= "connection-queue-size";
    public static final String GLOBAL_AGENT_ADDRESS 	= "global-agent-address";
    public static final String LOCAL_AGENT_ADDRESS 		= "local-agent-address";
    public static final String LOCAL_AGENT_JAR    		= "local-agent-jar";
    public static final String THREAD_POOL_SIZE 		= "thread-pool-size";
    public static final String CDWP_PORT 				= "cdwp-port";
    public static final String CONN_POOL_SIZE           = "connection-pool-size";
    public static final String JDWP_PORT                = "jdwp-port";
    public static final String STUB_LIST 				= "stublist";
    public static final String SKELETON_LIST 			= "skeletonlist";
    public static final String CORBA_ENABLED 			= "CORBA-enabled";
    public static final String GUID_ATTRIBUTE 			= "guid";
    public static final String PROC_SERVER_PORT         = "process-server-port";
    public static final String CALLBACK_OBJECT_PATH     = "callback-object-path";
    public static final String RMI_REGISTRY_PORT        = "rmi-registry-port";
    public static final String PROC_SERVER_JAR          = "process-server-jar";
    public static final String LOG4J_CONFIG_URL         = "log4j-config-url";
    public static final String INSTANCE_ID              = "instance-id";
    public static final String ROOT_TYPE                = "dd_config";
    
    /** Plugin-exclusive configuration attributes */
    public static final String ID                               = "ddproto1.plugin.core";
    public static final String ID_GLOBAL_AGENT_APPLICATION      = ID + ".centralAgentLaunchConfiguration";
    public static final String ID_GOD_DISTRIBUTED_APPLICATION   = ID + ".distributedNodeLaunchConfiguration";
    public static final String NAME_ATTRIBUTE                   = "name";
    public static final String BOOTSTRAP_PROPERTIES_FILE        = "bootstrap.properties";
    public static final String CENTRAL_AGENT_CONFIG_NAME        = "Distributed System";

    
    /** Attribute value constants. */
    public static final String TRUE                         = "yes";
    public static final String FALSE                        = "no";
    public static final String AUTO                         = "auto";

    /** Misc constants */
    public static final String XML_FILE_EXTENSION           = "xml";
    public static final String FILE_URL_PREFIX              = "file:";
    public static final String EXTENSION_SEPARATOR_CHAR     = ".";
    public static final String LIST_SEPARATOR_CHAR          = ";";
    public static final String URL_FILE_PROTOCOL            = "file";
    public static final String SERVICE_LOCATOR              = "service locator";
    public static final String LOCAL_AGENT_GID_OPT          = "agent.local.gid";
    public static final String LOCAL_AGENT_GA_ADDRESS_OPT   = "agent.global.address";
    public static final String LOCAL_AGENT_CONNPOOL_OPT     = "connection.pool.size";
    public static final String LOCAL_AGENT_LOG4J_OPT        = "log4.configuration.url";

    
    /** Keys into the preferences dictionary. */
    public static final String SPECS_DIR                    = "specs-dir";
    public static final String TOC_DIR                      = "toc-dir";
    public static final String LOG_PROPERTIES_FILE          = "log-properties-file";
    
}
