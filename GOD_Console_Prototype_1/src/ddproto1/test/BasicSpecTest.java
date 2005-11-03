/*
 * Created on Sep 5, 2005
 * 
 * file: BasicSpecTestCase.java
 */
package ddproto1.test;

import java.io.File;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.interfaces.IMessageBox;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

public class BasicSpecTest extends TestCase{
    
    protected String basedir;
    protected MessageHandler mh;
    protected String TOCurl;

    public void setUp(){
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
        
        mh = MessageHandler.getInstance();
        mh.setErrorOutput(stderr);
        mh.setStandardOutput(stdout);
        
        
        try{
            basedir = System.getProperty("user.dir");
            
            String separator = File.separator;
            if(!basedir.endsWith(separator)) basedir += separator;

            /** Creates a new XML configuration parser, assuming that all constraint
             * specs are located in basedir/SPECS_DIR, including the TOC.
             */
            TOCurl = "file://" + basedir + IConfigurationConstants.SPECS_DIR;
            System.setProperty("spec.lookup.path", "file:///;" + TOCurl);
        }catch(Exception ex){
            ex.printStackTrace();
            fail();
        }
    }

}
