/*
 * Created on Oct 18, 2005
 * 
 * file: ObjectSpecFactoryTest.java
 */
package ddproto1.configurator.test;

import java.net.URL;
import java.util.Iterator;

import org.xml.sax.SAXParseException;

import ddproto1.configurator.ChildrenDFSIterator;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.ObjectSpecStringfier;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.XMLConfigurationParser;
import ddproto1.util.Base64Encoder;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestUtils;


public class ObjectSpecFactoryTest extends BasicSpecTest {
    private static MessageHandler mh = MessageHandler.getInstance();
    public void testFactory(){
        try{
            SpecLoader sLoader = getDefaultSpecLoader();
            XMLConfigurationParser cfg = new XMLConfigurationParser(sLoader);
            
            String mainConfBaseURL = 
                TestUtils.getProperty(TestUtils.TESTS_DIR) + "/" +
                TestUtils.getProperty(TestUtils.RESOURCES_DIR) + "/";
            
            /** Parses the configuration file */
            IObjectSpec root = cfg.parseConfig(TestUtils.getResource(
                    mainConfBaseURL + DD_CONFIG_FILENAME));
            assertTrue(root.equals(root));
            ObjectSpecStringfier osFactory = new ObjectSpecStringfier(sLoader, new Base64Encoder());
            
            String stringRepresentation = osFactory.makeFromObjectSpec(root);
            IObjectSpec rootAlterego = osFactory.restoreFromString(stringRepresentation);
            
            System.out.println(XMLParserTest.stringHierarchy(root, "",""));
            System.err.println(XMLParserTest.stringHierarchy(rootAlterego, "",""));

            assertTrue(rootAlterego.isEquivalentTo(root));
            

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
}
