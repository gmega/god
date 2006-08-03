/*
 * Created on Jul 12, 2006
 * 
 * file: ConfiguratorSetup.java
 */
package ddproto1.launcher.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.xml.sax.SAXException;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.BundleEntryLister;
import ddproto1.configurator.FileLister;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.XMLConfigurationParser;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.util.TestUtils;

public class ConfiguratorSetup {
    
    public static SpecLoader configureSpecLoader(String specsDirList, String tocLocation)
        throws ExecutionException, MalformedURLException, SAXException, InterruptedException
    {
        List<String> specLocations = new ArrayList<String>(1);
        
        /** Retrieves the spes dir list. **/
        for(String specsDir : specsDirList.split(IConfigurationConstants.LIST_SEPARATOR_CHAR)){
            specLocations.add(TestUtils.getResource(specsDir).toExternalForm());
        }
        
        /** Creates a new Spec Loader **/
        SpecLoader sLoader = new SpecLoader(specLocations, TestUtils.getResource(tocLocation).toExternalForm());
        if(TestUtils.isPluginTest())
            sLoader.registerURLLister(new BundleEntryLister(GODBasePlugin.getDefault().getBundle()));
        sLoader.registerURLLister(new FileLister());
        
        return sLoader;
    }
    
    public static IObjectSpec getRoot()
        throws ExecutionException, MalformedURLException, SAXException, InterruptedException{
        
        /** Creates a new Spec Loader **/
        SpecLoader sLoader = configureSpecLoader(TestUtils.getProperty(TestUtils.COMP_SPECS_DIR), TestUtils.getProperty(TestUtils.COMP_TOC_DIR));
        XMLConfigurationParser xcp =
            new XMLConfigurationParser(sLoader);
                
        return xcp.parseConfig(TestUtils.getResource(TestUtils
                .getProperty(TestUtils.COMP_CONF_FILE)), TestUtils.getTestPropertiesCopy());
    }
}
