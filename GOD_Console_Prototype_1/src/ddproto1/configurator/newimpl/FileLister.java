/*
 * Created on Oct 15, 2005
 * 
 * file: FileLister.java
 */
package ddproto1.configurator.newimpl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ddproto1.configurator.commons.IConfigurationConstants;

public class FileLister implements IURLLister{
    
    private static Logger logger = Logger.getLogger(FileLister.class);
       
    public List<URL> list(URI source) throws IllegalProtocolException {

        if (!source.getScheme().equals(IConfigurationConstants.URL_FILE_PROTOCOL))
            throw new IllegalProtocolException("FileLister only supports "
                    + IConfigurationConstants.URL_FILE_PROTOCOL + " urls.");
        
        URL url;
        try{
            url = source.toURL();
        }catch(MalformedURLException ex){
            throw new IllegalProtocolException("URL is malformed", ex);
        }
                
        List<URL> fileURLs = new ArrayList<URL>();

        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            logger.error("Failed to list subfiles of " + source, e);
            return fileURLs;
        }
        
        if(f.exists()){
            for(File file : f.listFiles()){
                try{
                    fileURLs.add(file.toURL());
                }catch(MalformedURLException ex){
                    logger.error("Could not convert file " + file + " to URL. It's been ignored.", ex);
                }
            }
        }
        
        return fileURLs;
    }

    public URL getEntry(URI source, String entryName) throws MalformedURLException{
        return new URL(source.toURL(), entryName);
    }
}
