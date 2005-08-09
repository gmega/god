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

import ddproto1.Main;
import ddproto1.configurator.newimpl.IConfigurationConstants;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.newimpl.XMLConfigurationParser;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class XMLParserTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(XMLParserTest.class);
    }

    public void testParseConfig() {
        
        // This is required for all test cases.
        IMessageBox stdout = new IMessageBox(){
            public void println(String s){
                System.out.println(s);
            }
            
            public void print(String s){
                System.out.print(s);
            }
        };
        
        IMessageBox stderr = new IMessageBox(){
            public void println(String s){
                System.err.println(s);
            }
            
            public void print(String s){
                System.err.print(s);
            }
        };
        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setErrorOutput(stderr);
        mh.setStandardOutput(stdout);
        
        
        try{
            String basedir = System.getProperty("user.dir");
            String separator = File.separator;
            if(!basedir.endsWith(separator)) basedir += separator;

            /** Creates a new XML configuration parser, assuming that all constraint
             * specs are located in basedir/SPECS_DIR, including the TOC.
             */
            String url = "file://" + basedir + IConfigurationConstants.SPECS_DIR;
            List <String> specPath = new ArrayList<String>();
            specPath.add(url);
            XMLConfigurationParser cfg = new XMLConfigurationParser(new SpecLoader(specPath, url));
            
            /** Parses the configuration file */
            IObjectSpec root = cfg.parseConfig(new URL("file://" + basedir + Main.DD_CONFIG_FILENAME)); 
        
            System.out.println(" -- Info Summary --");
            this.printHierarchy(root, "","");
            

        }catch(Exception e){
            mh.getErrorOutput().println(e.getMessage());
            mh.printStackTrace(e);
        }
 
    }
    
    public String printHierarchy(IObjectSpec spec, String initialSpacing, String fLine){
        
        StringBuffer spaces = new StringBuffer();
        
        try{
            if(fLine != null) { spaces.append(fLine); fLine = null; }
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
            spaces.append(printHierarchy(child, initialSpacing + " |  ", "[+]-"));
        }
        
        spaces.append(initialSpacing + " |> \n");
        
        return spaces.toString();
    }

}
