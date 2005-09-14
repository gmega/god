/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DefaultConfiguratorImplTest.java
 */

package ddproto1.test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXParseException;

import ddproto1.Main;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.newimpl.XMLConfigurationParser;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class XMLParserTest extends BasicSpecTest {

    private IObjectSpec root;
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(XMLParserTest.class);
    }
    
    public void testParseConfig() {
        
        try{

            XMLConfigurationParser cfg = new XMLConfigurationParser(new SpecLoader(null, TOCurl));
            
            /** Parses the configuration file */
            IObjectSpec root = cfg.parseConfig(new URL("file://" + basedir + Main.DD_CONFIG_FILENAME)); 
        
            System.out.println(" -- Info Summary --");
            mh.getStandardOutput().print(this.stringHierarchy(root, "",""));
            
            this.root = root;
            
            /** Deep context test */
            root = root.getChildOfType(IConfigurationConstants.NODE_LIST);
            List<IObjectSpec> children = root.getChildren();
            IObjectSpec launcher = children.get(0).getChildOfType("launcher");
            try{
                launcher.getAttribute("cdwp-port");
            }catch(UninitializedAttributeException ex){ }
            

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
    
    public String stringHierarchy(IObjectSpec spec, String initialSpacing, String fLine){
        
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

}
