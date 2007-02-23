/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DefaultConfiguratorImplTest.java
 */

package ddproto1.configurator.test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.xml.sax.SAXParseException;

import ddproto1.configurator.ConfiguratorUtils;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.XMLConfigurationParser;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestConfigurationConstants;
import ddproto1.util.TestUtils;

/**
 * @author giuliano
 *
 */
public class XMLParserTest extends BasicSpecTest {

    private static MessageHandler mh = MessageHandler.getInstance();
    
    private IObjectSpec root;
    
    public void testParseConfig() {
        
        try{

            XMLConfigurationParser cfg = new XMLConfigurationParser(getDefaultSpecLoader());
            
            URL url = Thread.currentThread().getContextClassLoader().getResource(
                    TestUtils.getProperty(TestUtils.TESTS_DIR) + "/" + 
                    TestUtils.getProperty(TestUtils.RESOURCES_DIR) + "/" +
                    DD_CONFIG_FILENAME);
            
            /** Parses the configuration file */
            IObjectSpec root = cfg.parseConfig(url); 
        
            System.out.println(" -- Info Summary --");
            String h1 = XMLParserTest.stringHierarchy(root, "","");
            mh.getStandardOutput().print(h1);
            Writer writer = new OutputStreamWriter(new FileOutputStream("/home/giuliano/l1"));
            writer.write(h1);
            writer.flush();
            writer.close();
            
            this.root = root;
            
            /** Deep context test */
            root = root.getChildOfType(IConfigurationConstants.NODE_LIST);
            List<IObjectSpec> children = root.getChildren();
            IObjectSpec launcher = children.get(0).getChildOfType("launcher");
            try{
                launcher.getAttribute("callback-object-path");
            }catch(UninitializedAttributeException ex){ }

            url = Thread.currentThread().getContextClassLoader().getResource(
                    TestUtils.getProperty(TestUtils.TESTS_DIR) + "/" + 
                    TestUtils.getProperty(TestUtils.RESOURCES_DIR) + "/" +
                    DD_CONFIG_TEMPLATED_FILENAME);
            
            root = cfg.parseConfig(url, getProperties());
            
            String h2 = stringHierarchy(root, "", "");
            System.out.println(h2);
            writer = new OutputStreamWriter(new FileOutputStream("/home/giuliano/l2"));
            writer.write(h2);
            writer.flush();
            writer.close();
            
            assertTrue(h1.equals(h2));
            
        }catch(Exception e){
            mh.getErrorOutput().println(e.getMessage());
            if(e instanceof SAXParseException){
                SAXParseException sax = (SAXParseException)e;
                mh.getErrorOutput().println("Line: " + sax.getLineNumber() + " Column: " + sax.getColumnNumber());
            }
            mh.printStackTrace(e);
            fail();
        }
 
    }
    
    public IObjectSpec getRoot(){
        return root;
    }
    
    public static String stringHierarchy(IObjectSpec spec, String initialSpacing, String fLine){
        
        StringBuffer spaces = new StringBuffer();
        
        try{
            if(fLine != null) { spaces.append(fLine); fLine = fLine + "-+--"; }
            else spaces.append(initialSpacing);
            spaces.append("[+]" + " ObjectSpec type: " + spec.getType().getInterfaceType() + "\n");
        }catch(IllegalAttributeException e){ fail();}
        
        for(String key : spec.getType().attributeKeySet()){
            String value = null;
            try{
                value = spec.getAttribute(key);
            }catch(UninitializedAttributeException ex){
                value = "<uninitialized>";
            }catch(IllegalAttributeException ex){
                value = "<unsupported>";
            }
            
            String attribute = key + ": " + value + "\n";
            
            spaces.append(initialSpacing + " | " + attribute);
        }
        
        for(IObjectSpec child : spec.getChildren()){
            spaces.append(stringHierarchy(child, initialSpacing + " |  ", fLine));
        }
        
        spaces.append(initialSpacing + " \n");
        
        return spaces.toString();
    }

    private Properties getProperties() throws Exception {
        Properties props = new Properties();
        props.load(TestUtils.getResource(
                TestUtils.getProperty(TestConfigurationConstants.TESTS_DIR) + "/" +
                TestUtils.getProperty(TestConfigurationConstants.RESOURCES_DIR) + "/" + 
                PARSER_TEST_ABS_PROPS_FILENAME).openStream());
        
        InputStream iStream = TestUtils
                .getResource(
                        TestUtils.getProperty(TestConfigurationConstants.TESTS_DIR) + "/" + 
                        TestUtils.getProperty(TestConfigurationConstants.RESOURCES_DIR) + "/" + 
                        PARSER_TEST_REL_PROPS_FILENAME).openStream();
        
        String replaced = ConfiguratorUtils.tokenReplace(iStream, props);
        Properties fullProps = new Properties();
        fullProps.load(new ByteArrayInputStream(replaced.getBytes()));
        fullProps.putAll(props);
        
        return fullProps;
    }
}
