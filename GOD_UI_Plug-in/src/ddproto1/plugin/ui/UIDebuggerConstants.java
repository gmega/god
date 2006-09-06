/*
 * Created on Oct 13, 2005
 * 
 * file: IDebuggerConstantsUI.java
 */
package ddproto1.plugin.ui;

public interface UIDebuggerConstants {

    /** Bootstrap properties file name. */
    public static final String BOOTSTRAP_PROPERTIES_FILE="bootstrap.properties";
    
    /** Keys into the preferences dictionary. */
    public static final String LOG_PROPERTIES_FILE  = "log4j-config-filename";
    public static final String PREFERENCE_PAGES_FILE="preference-pages-file";

    public static final String CONFIGURATION_USERS_EP = "ddproto1.plugin.ui.configurationUser";
    public static final String CLASS_PROPERTY = "class"; 
    
    /** Status codes. */
    public static final int STATS_NO_ROOT_IMPL = -1;
    
    /** Keys into the ILaunchConfiguration attribute dictionaries. */ 
    public static final String ROOT_ATTRIBUTE = "root-attribute";
    
    /** Images */
    public static final String IMAGES_DIR = "images";
    public static final String CONTEXT_ICON = IMAGES_DIR + "/ctx.png";
    public static final String SPECIFICATION_ICON = IMAGES_DIR + "/spec.png";
    public static final String VIRTUAL_STACKFRAME_ICON = IMAGES_DIR + "/virtual_stackframe.png";
    public static final String VIRTUAL_STACKFRAME_DMG_ICON = IMAGES_DIR + "/virtual_stackframe_damaged.png";
    
}
 