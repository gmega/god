/*
 * Created on Apr 27, 2005
 * 
 * file: AttributeConstants.java
 */
package ddproto1.configurator.commons;

import java.io.File;

public interface IConfigurationConstants {
    // TODO: Move all this stuff to a configuration file (maybe the TOC?).
    /** XML structural constants */
    public static final String SPEC_ATTRIB = "spec";
    public static final String TYPE_ATTRIB = "type";
    public static final String NAME_ATTRIB = "name";
    public static final String CHILD_ATTRIB = "child";
    public static final String SPEC_EXT_ATTRIB = "spec-extension";
    public static final String OPTIONAL_ATTRIB = "extension-attribute";
    public static final String PROPERTY_ATTRIB = "attribute";
    public static final String ID_ATTRIB = "id";
    public static final String ACTION_ATTRIBUTE = "action";
    public static final String ROOT_ATTRIB = "root-element";
    public static final String ELEMENT_ATTRIB = "element";
    public static final String MULTIPLICITY_ATTRIB = "multiplicity";
    public static final String INTENDED_INTF_ATTRIB = "intended-interfaces";
    public static final String DEFAULT_VALUE_ATTRIB = "default-value";
        
    public static final String PARAM_ATTRIB = "param";
    public static final String PARAM_KEY_ATTRIB = "key";
    public static final String PARAM_VAL_ATTRIB = "value";
    
    /** Client constants (attribute keys, essentially). */
    public static final String NODE_LIST = "node-list";
    public static final String NODE = "node";
    public static final String PORT = "cdwp-port";
    public static final String MAX_QUEUE_LENGTH = "connection-queue-size";
    public static final String GLOBAL_AGENT_ADDRESS = "global-agent-address";
    public static final String GUID = "guid";
    public static final String STUB_LIST = "stublist";
    public static final String SKELETON_LIST = "skeletonlist";
    public static final String CORBA_ENABLED = "CORBA-enabled";
    
    public static final String TRUE = "yes";
    public static final String FALSE = "no";
    public static final String AUTO = "auto";

    /** Directory constraints. */
    public static final String SPECS_DIR = "specs";
    public static final String TOC_DIR = "specs";
    public static final String JAVA_TRANSLATIONS_DIR = SPECS_DIR + File.separator + "java_translations";
    public static final String TRANSLATION_FILE_EXTENSION = "translation";
    public static final String TRANSLATION_TOC_FILENAME = "TOC" + "." + TRANSLATION_FILE_EXTENSION;
    
    /** Misc constants */
    public static final String XML_FILE_EXTENSION = "xml";
    public static final String FILE_URL_PREFIX = "file:";
    public static final String EXTENSION_SEPARATOR_CHAR=".";
    public static final String LIST_SEPARATOR_CHAR = ";";
    public static final String URL_FILE_PROTOCOL="file";
}
