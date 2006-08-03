/*
 * Created on Sep 5, 2005
 * 
 * file: BasicSpecTestCase.java
 */
package ddproto1.configurator.test;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ddproto1.configurator.SpecLoader;
import ddproto1.interfaces.IMessageBox;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.ILogManager;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestUtils;

public class BasicSpecTest extends TestCase{
    
    protected MessageHandler mh;

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
        
        BasicConfigurator.configure();
        
        mh.setLogManagerDelegate(new ILogManager(){
            public Logger getLogger(Class c) {
                return Logger.getLogger(c);
            }
            public Logger getLogger(String name) {
                return Logger.getLogger(name);
            }
        });
    }
    
    protected SpecLoader getDefaultSpecLoader()
        throws Exception
    {
        return ConfiguratorSetup.configureSpecLoader(
                TestUtils.getProperty(TestUtils.COMP_SPECS_DIR), 
                TestUtils.getProperty(TestUtils.MAIN_DIR) + "/" + 
                TestUtils.getProperty(TestUtils.CONFIG_DIR) + "/" +
                TestUtils.getProperty(TestUtils.SPECS_DIR));
    }

}
