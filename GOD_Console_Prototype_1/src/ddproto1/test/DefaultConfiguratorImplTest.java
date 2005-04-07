/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DefaultConfiguratorImplTest.java
 */

package ddproto1.test;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import ddproto1.configurator.DefaultConfiguratorImpl;
import ddproto1.configurator.IConfigurator;
import ddproto1.configurator.NodeInfo;
import ddproto1.interfaces.IMessageBox;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class DefaultConfiguratorImplTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultConfiguratorImplTest.class);
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
        
        IConfigurator c = new DefaultConfiguratorImpl();
        String current = System.getProperty("user.dir");
        
        try{
        
            Collection nodes = c.parseConfig(new URL("file://" + current + "/dd_config.xml"));
    
            System.out.println(" -- Info Summary --");
            Iterator it = nodes.iterator();
            while(it.hasNext()){
                NodeInfo info = (NodeInfo)it.next();
                String[] attribset = info.getAttributeKeys();
                System.out.println("**** NODE ****");
                for(int i = 0; i < attribset.length; i++){
                    String val = (String)info.getAttribute(attribset[i]);
                    System.out.println("(Key - " + attribset[i] + "), (value - " + val + ")");
                }
                System.out.println("**************");
                
            }
        }catch(Exception e){
            mh.printStackTrace(e);
        }
 
    }

}
