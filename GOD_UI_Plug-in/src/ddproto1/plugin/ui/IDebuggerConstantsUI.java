/*
 * Created on Oct 13, 2005
 * 
 * file: IDebuggerConstantsUI.java
 */
package ddproto1.plugin.ui;

public interface IDebuggerConstantsUI {

    /** Bootstrap properties file name. */
    public static final String BOOTSTRAP_PROPERTIES_FILE="bootstrap.properties";
    
    /** Keys into the preferences dictionary. */
    public static final String SPECS_SUBPATH_LIST   = "spec-dir-list";
    public static final String TOC_SUBPATH          = "toc-subpath";
    public static final String NODE_CONFIG_TYPE     = "node-config-type";
    public static final String NODE_LIST_TYPE       = "node-list-type";
    public static final String ROOT_TYPE            = "root-spec-type";
    public static final String LOG_PROPERTIES_FILE  = "log4j-config-filename";
    
    public static final String DEFAULT_THREAD_POOL   = "20";
    public static final String DEFAULT_CDWP_PORT     = "8080";
    public static final String DEFAULT_CONN_QUEUE    = "10";
    public static final String DEFAULT_GA_ADDRESS     = "localhost";
    
    public static final String CONFIGURATION_USERS_EP = "ddproto1.plugin.ui.configurationUser";
    public static final String CLASS_PROPERTY = "class"; 
    
    /** Status codes. */
    public static final int STATS_NO_ROOT_IMPL = -1;
    
    /** Keys into the ILaunchConfiguration attribute dictionaries. */ 
    public static final String ROOT_ATTRIBUTE = "root-attribute";
    
}
 