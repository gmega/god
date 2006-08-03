/*
 * Created on Oct 15, 2005
 * 
 * file: BundleEntryLister.java
 */
package ddproto1.configurator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;

import ddproto1.configurator.IURLLister;
import ddproto1.configurator.IllegalProtocolException;

public class BundleEntryLister implements IURLLister{

    public static final String BUNDLE_URL_PROTOCOL = "bundleentry";
    
    private Bundle root;
    
    public BundleEntryLister(Bundle root){
        this.root = root;
    }
    
    public String protocol(){
    		return BUNDLE_URL_PROTOCOL;
    }
    
    public List<URL> list(URI source) throws IllegalProtocolException {
        
        if(!source.getScheme().equals(BUNDLE_URL_PROTOCOL)) 
            throw new IllegalProtocolException("BundleEntryLister only understands the " 
                    + BUNDLE_URL_PROTOCOL + " protocol.");
        
        Enumeration<URL> _enum = root.findEntries(source.getPath(), "*", false);
        List<URL> entries = new ArrayList<URL>();
        while(_enum.hasMoreElements()) entries.add(_enum.nextElement());
        
        return entries;
    }

    public URL getEntry(URI source, String entryName) throws MalformedURLException {
        if(!source.getScheme().equals(BUNDLE_URL_PROTOCOL)) 
            throw new MalformedURLException("BundleEntryLister only understands the " 
                    + BUNDLE_URL_PROTOCOL + " protocol.");
        
        String path = source.getPath();
        path = (path.endsWith("/"))?path:path + "/";
        return root.getEntry(path + entryName);
    }
    
}
