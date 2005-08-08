/*
 * Created on May 18, 2005
 * 
 * file: IOCTest.java
 */
package ddproto1.configurator.ioc.testclasses;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ddproto1.configurator.IConfigurator;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IServiceLocator;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.newimpl.StandardServiceLocator;
import ddproto1.configurator.newimpl.XMLConfigurationParser;
import ddproto1.interfaces.IMessageBox;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

public class IOCTest extends TestCase {

    public void testIOC() throws Exception{
        
        /** I hate setting the message handler but it's necessary. Maybe I should think about 
         * adding some reasonable defaults.
         */
        IMessageBox mb = new IMessageBox(){
            public void println(String s) {
                System.out.println(s);
            }

            public void print(String s) {
                System.out.print(s);
            }
        };
        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setDebugOutput(mb);
        mh.setErrorOutput(mb);
        mh.setStandardOutput(mb);
        mh.setWarningOutput(mb);
        
        String toc = "file:///home/giuliano/workspace/GOD Console Prototype 1/testspecs";
        
        /** Begins by reading stuff from the configuration file. */
        List<String> locations = new ArrayList<String>();
        locations.add(toc);
        SpecLoader sloader = new SpecLoader(locations, toc);
        IConfigurator configurator = new XMLConfigurationParser(sloader);
        
        IObjectSpec root = configurator.parseConfig(new URL("file:///home/giuliano/workspace/GOD Console Prototype 1/testconfig.xml"));
        
        /** Now, to the service locator. */
        IServiceLocator svc = StandardServiceLocator.getInstance();
        
        /** We know that the root has a list. We're interested on the list elements. */
        List<IObjectSpec> models = root.getChildren().get(0).getChildren(); // Ugly but quick.
        List<Civic> vehicles = new ArrayList<Civic>();
        
        for(IObjectSpec civicSpec : models){
            vehicles.add((Civic)svc.incarnate(civicSpec));
        }
        
        /** Start them all */
        for(Civic civic : vehicles){
            civic.start();
        }
    }

}
