/*
 * Created on Aug 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: URLSourceMapper.java
 */

package ddproto1.sourcemapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;

import ddproto1.configurator.IConfigurable;
import ddproto1.configurator.IConfigurator;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.UninitializedAttributeException;
import ddproto1.util.MessageHandler;

/**
 * This source mapper is based on URLs for easier integration into distributed
 * environments. It suffers from a problem, though, that you should be aware of.
 * If you specify two or more sourcepaths containing the same source file (under
 * the same full qualified namespace structure) there will be no guarantees as to
 * which source will be used when finding a given source location.  
 * 
 * @author giuliano
 *
 */
public class URLSourceMapper implements ISourceMapper, IConfigurable{

    private static final String SOURCE_PATH="sourcepath";
    
    // I don't know if url separators are system-dependent.
    private static final String separator = "/";
    
    private Set sourceURLs;
    private Map name2source;
    private SourceFactory srcfactory;
    private String originalSourcepath;
   
    private static final String module = "URLSourceMapper -";
    
    public URLSourceMapper(){ 
        // TODO Implement default interface implementation classes at a central location.
        this.srcfactory = new SourceCache();
        name2source = new HashMap();
        sourceURLs = new HashSet();
    }
    
    private void addSourceLocations(String sourcepath){
        StringTokenizer strtk = new StringTokenizer(sourcepath, IConfigurator.LIST_SEPARATOR_CHAR);
        while(strtk.hasMoreTokens()){
            String url = strtk.nextToken();
            if(!url.endsWith(separator))
                url += separator;
            
            sourceURLs.add(url);
        }
                
    }
    
    private InputStream getSource(String sourcename){
        MessageHandler mh = MessageHandler.getInstance();
        Iterator it = sourceURLs.iterator();
        
        InputStream is = null;
        
        while(it.hasNext()){
            String path = (String)it.next();
            try{
                URL url = new URL(path + sourcename);
                is = url.openStream();
            }catch(MalformedURLException e){
                mh.getErrorOutput().println(module + " Malformed URL " + path + " will be removed from source path.");
                sourceURLs.remove(path);
            // An I/O exception might just mean we should look somewhere else.   
            }catch(IOException e) { }
        }
        
        return is; 
    }

    
    public void setSourceFactory(SourceFactory sf){
        this.srcfactory = sf;        
    }
    
   
    /* (non-Javadoc)
     * @see ddproto1.interfaces.SourceMapper#getLine(com.sun.jdi.Location)
     */
    public String getLine(Location loc) {
        
        String name = loc.declaringType().name();
        
        // Searches the cache.
        if(name2source.containsKey(name)){
            ISource src = (ISource)name2source.get(loc);
            String line = src.getLine(loc.lineNumber());
            if(line == null)
                return "Can't find source line " + loc.lineNumber() + " for "
                        + name + ". Are your binaries up-to-date?";
        }
        
        try{
            InputStream is = locateSource(loc);
            if(is == null)
                return "Could not locate source file " + loc.sourceName();
            
            ISource s = srcfactory.make(is, name);
            return s.getLine(loc.lineNumber());
        }catch(AbsentInformationException e){
            return "Could not determine source file for " + name;
        }catch(IOException e){
            MessageHandler mh = MessageHandler.getInstance();
            mh.printStackTrace(e);
            return "Error while reading from input stream. Maybe your URL is outdated or source server is down.";
        }
    }
    
    public ISource getSource(Location loc)
    	throws AbsentInformationException, IOException
    {
        String name = loc.declaringType().name();
        if(name2source.containsKey(name)){
            ISource src = (ISource)name2source.get(loc);
            return src;
        }
        InputStream is = locateSource(loc);
        if(is == null)
            throw new AbsentInformationException("Could not locate source file " + loc.sourceName());
        
        ISource s = srcfactory.make(is, name);
        return s;
    }
    
    private InputStream locateSource(Location loc)
    	throws AbsentInformationException, IOException
    {
        String name = loc.declaringType().name();
        
        int iDot = name.lastIndexOf('.');
        String appender;        
        if(iDot == -1)
            appender = "";
        else{
            appender = name.substring(0, iDot + 1);
            appender = appender.replace('.', separator.charAt(0));
        }
        
        InputStream is = getSource(appender + loc.sourceName());
        
        return is;
    }

    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
        if(key.equals(SOURCE_PATH)) return originalSourcepath;
        else throw new IllegalAttributeException("URLSourceMapper - Unrecognized attribute " + key);
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        if(key.equals(SOURCE_PATH)){
            this.addSourceLocations(val);
        }else{
            throw new IllegalAttributeException("URLSourceMapper - Unrecognized attribute " + key);
        }
    }

    public boolean isWritable() {
        return true;
    }
}
