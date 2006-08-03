/*
 * Created on Sep 5, 2005
 * 
 * file: FSImplementationScanner.java
 */
package ddproto1.configurator.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.xml.sax.SAXException;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.ISpecLoader;
import ddproto1.configurator.ISpecType;
import ddproto1.configurator.IURLLister;
import ddproto1.configurator.IllegalProtocolException;
import ddproto1.configurator.SpecNotFoundException;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.UnorderedMultiMap;
import ddproto1.util.traits.commons.ConversionUtil;

public class FSImplementationScanner implements IImplementationScanner {
    
    private static final String URL_FILE_SEPARATOR_CHAR = "/";
    
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    private final MessageHandler mh = MessageHandler.getInstance();
    
    private List<IImplementationScannerListener> listeners = 
        new ArrayList<IImplementationScannerListener>();
    
    private UnorderedMultiMap<String, IObjectSpecType> specCache;    
    private Set<String>parsedClasses = new HashSet<String>();
   
    private ISpecLoader specLoader;
    
    private Map<String, IURLLister> entryFinders = new HashMap<String, IURLLister>();
    
    public FSImplementationScanner(boolean doCaching, ISpecLoader specLoader){
        if(doCaching) specCache = new UnorderedMultiMap<String, IObjectSpecType>(
                HashSet.class);
        parsedClasses = new HashSet<String>();
        this.specLoader = specLoader;
    }

    public void asyncRetrieveImplementationsOf(String interfaceSet) {
        
        final String iSet = interfaceSet;
        
        /** If things are cached then we can retrieve the 
         * list really fast (I'm trusting that the listeners
         * won't take forever to process the notifications).
         */
        if(specCache != null && parsedClasses.contains(iSet)){
            Iterable<IObjectSpecType>specCacheList = specCache.get(iSet);
            broadcast((specCacheList == null)?new ArrayList<IObjectSpecType>():specCacheList);
            return;
        }
        
        /** Disk access will be necessary. */
        Thread asynchProcessor = new Thread(new Runnable(){
            public void run() {
                try {
                    broadcast(retrieveImplementationsOf(iSet));
                } catch (Exception ex) {
                    mh.printStackTrace(ex);
                }
            }
        });
        
        asynchProcessor.start();
    }
    
    public Iterable<IObjectSpecType> retrieveImplementationsOf(String iSet)
    		throws IOException, SpecNotFoundException, SAXException, IllegalProtocolException
    {
    		ISpecType type = specLoader.specForName(iSet);
        String lookupURLs = System.getProperty("spec.lookup.path");

        List <IObjectSpecType> matchingSpecs = new ArrayList<IObjectSpecType>();
        
        /** Nothing to lookup. Broadcast an empty list. */
        if (lookupURLs == null) {
            mh.getWarningOutput().println(
                    "System property spec.lookup.path hasn't been set.");
            broadcast(matchingSpecs);
        }

        /** Adds all matching specs present in the spec lookup path. */
        StringTokenizer strtok = new StringTokenizer(lookupURLs,
                IConfigurationConstants.LIST_SEPARATOR_CHAR);

        while (strtok.hasMoreElements())
            addMatchingSpecs(matchingSpecs,
                    strtok.nextToken(), type);
        
        /** Add to cache. */
        if(specCache != null){
            for(IObjectSpecType specData : matchingSpecs){
                specCache.add(iSet, specData);
                parsedClasses.add(iSet);
            }
        }
        
        return matchingSpecs;
    }
    
    public synchronized void registerURLLister (IURLLister lister){
    		entryFinders.put(lister.protocol(), lister);
    }
    
    private synchronized IURLLister getListerFor(String protocol)
    		throws IllegalProtocolException{
    		if(!entryFinders.containsKey(protocol))
    			throw new IllegalProtocolException("Unknown URL protocol " + protocol);
    		
    		return entryFinders.get(protocol);
    }
    
    private void addMatchingSpecs(List<IObjectSpecType> matchingSpecs,
			String url, ISpecType type) 
    		throws IllegalProtocolException
    {
		List<URL> filenamesToProcess;
		
		try {
			URI uri = ct.makeEncodedURI(url) ;
			IURLLister lister = getListerFor(uri.getScheme());
			filenamesToProcess = lister.list(uri);
		} catch (URISyntaxException ex) {
			mh.getErrorOutput().println(
					"Invalid URI " + url + " ignored.");
			return;
		}

		String iSetAlias = type.getExtension();

		/** Processes all urls. */
		for (URL specURL : filenamesToProcess) {
			String filename = specURL.getFile();
			
			/** Chops off everything until the last path separator. */
			int idx = filename.lastIndexOf(URL_FILE_SEPARATOR_CHAR);
			if(idx != -1)
				filename = filename.substring(idx + 1, filename.length());
			
			/** Starts by trying to extract an extension */
			idx = filename
					.lastIndexOf(IConfigurationConstants.EXTENSION_SEPARATOR_CHAR);
			if (idx == -1)
				continue;
			/** No extension - bye-bye */

			String extension = filename.substring(idx + 1, filename.length());

			/** Wrong extension - bye-bye */
			if (!extension.equals(IConfigurationConstants.XML_FILE_EXTENSION))
				continue;

			/** Extracts the specification type part and the concrete type. */
			filename = filename.substring(0, idx);
			String specType = null;
			String specConcrete = null;
			idx = filename
					.lastIndexOf(IConfigurationConstants.EXTENSION_SEPARATOR_CHAR);

			if (idx == -1)
				specType = filename;
			else {
				specType = filename.substring(idx + 1, filename.length());
				specConcrete = filename.substring(0, idx);
			}

			/** If it doesn't match what we're interested in, just let it. */
			if (!specType.equals(iSetAlias))
				continue;

			try {
				IObjectSpecType spec = specLoader.specForName(specConcrete,
						type);
				if (!spec.isExtension())
					matchingSpecs.add(spec);
			} catch (Exception ex) {
				mh.getErrorOutput().println(
						"Could not read spec type <" + specConcrete + ", "
								+ specType + ">");
				ex.printStackTrace();
			}

		}
	}

    
    public void flushCache(){
        parsedClasses.clear();
    }

    private synchronized void broadcast(Iterable<IObjectSpecType> theList){
        for(IImplementationScannerListener listener : listeners){
            listener.receiveAnswer(theList);
        }
    }
    
    public synchronized void addAnswerListener(IImplementationScannerListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeAnswerListener(IImplementationScannerListener listener) {
        listeners.remove(listener);   
    }
}
