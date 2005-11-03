/*
 * Created on Oct 20, 2005
 * 
 * file: SpecLoaderMaker.java
 */
package ddproto1.test.support;

import java.io.File;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.newimpl.ISpecLoader;
import ddproto1.configurator.newimpl.SpecLoader;

public class SpecLoaderMaker {
    
    public static ISpecLoader getSpecLoader(){
        String basedir = System.getProperty("user.dir");
    
        String separator = File.separator;
        if(!basedir.endsWith(separator)) basedir += separator;

        /** Creates a new XML configuration parser, assuming that all constraint
         * specs are located in basedir/SPECS_DIR, including the TOC.
         */
        String TOCurl = "file://" + basedir + IConfigurationConstants.SPECS_DIR;
        System.setProperty("spec.lookup.path", "file:///;" + TOCurl);
        
        return new SpecLoader(null, TOCurl);
    }
}
