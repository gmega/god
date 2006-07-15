/*
 * Created on Oct 20, 2005
 * 
 * file: DDUIPlugin.java
 */
package ddproto1.plugin.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

import com.tools.logging.PluginLogManager;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.ISpecLoader;
import ddproto1.configurator.ISpecType;
import ddproto1.configurator.ObjectSpecStringfier;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.SpecNotFoundException;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.plugin.core.DDCorePlugin;
import ddproto1.plugin.core.IDDCorePluginConstants;
import ddproto1.plugin.ui.launching.FSImplementationScanner;
import ddproto1.plugin.ui.launching.IImplementationScanner;
import ddproto1.plugin.ui.launching.listers.BundleEntryLister;
import ddproto1.util.Base64Encoder;
import ddproto1.util.MessageHandler;

public class DDUIPlugin extends AbstractUIPlugin{
    
    //The shared instance.
    protected static DDUIPlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;
    //Logger (lazily initialized) 
    private Logger logger;
    
    private ConfigManagerImpl configManager;
    private PluginLogManager  logManager;

    /**
     * The constructor.
     */
    public DDUIPlugin() {
        super();
        plugin = this;
    }

    
    public static DDUIPlugin getDefault(){
        return plugin;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        this.initManagers();
        this.setMessageBoxes();
    }

    
    private void initManagers() throws CoreException{
        configManager = new ConfigManagerImpl();
        configManager.start();
        this.configureLogManager();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        resourceBundle = null;
    }
    
    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = DDUIPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public ResourceBundle getResourceBundle() {
        try {
            if (resourceBundle == null)
                resourceBundle = ResourceBundle.getBundle("br.usp.ime.eclipse.ddproto1.DDUIPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        return resourceBundle;
    }
    
    public IConfigurationManager getConfigurationManager(){
        return configManager;
    }
    
    public PluginLogManager getLogManager(){
        return logManager;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("GOD_Prototype_1_UI_Plug_in", path);
    }
    
    
    private void setMessageBoxes(){
        IMessageBox stdout = new IMessageBox(){
            public void println(String s) { logger.info(s); }
            public void print(String s) { logger.info(s); }
        };
        
        IMessageBox stderr = new IMessageBox(){
            public void println(String s) { logger.error(s); }
            public void print(String s) { logger.error(s); }
        };
        
        IMessageBox warn = new IMessageBox(){
            public void println(String s) { logger.warn(s); }
            public void print(String s) { logger.warn(s); }
        };
        
        IMessageBox debug = new IMessageBox(){
            public void println(String s) { logger.debug(s); }
            public void print(String s) { logger.debug(s); }
        };

        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setWarningOutput(warn);
        mh.setStandardOutput(stdout);
        mh.setErrorOutput(stderr);
        mh.setDebugOutput(debug);
    }
    
    private class ConfigManagerImpl implements IConfigurationManager, INodeList, IPropertyChangeListener{

        private ISpecLoader             loader;
        private IImplementationScanner  scanner;
        private Properties              bootstrap;
        
        private IObjectSpec             rootSpec;
        private IObjectSpec             nodeList;
        
        private String                  attName;
        
        private ObjectSpecStringfier    encoder;
               
        private BidiMap name2Node = new DualHashBidiMap();
        private BidiMap node2Name = name2Node.inverseBidiMap();
        
        public void start()
            throws CoreException
        {
            /** Load bootstrap properties. */
            Bundle bundle = DDUIPlugin.this.getBundle();
            try{
                URL bootstrapURL = bundle.getEntry(IDebuggerConstantsUI.BOOTSTRAP_PROPERTIES_FILE);
                Properties boot = new Properties();
                boot.load(bootstrapURL.openStream());
                this.bootstrap = boot;
            }catch(Exception ex){
                throw new CoreException(
                    new Status(IStatus.ERROR, bundle.getSymbolicName(), IStatus.ERROR,
                            "Failed to load the bootstrap configuration file. Plug-in " +
                            "may not operate correctly.", null));
            }
            
            /** Apply bootstrap properties. */
            IPreferenceStore store = DDUIPlugin.this.getPreferenceStore();
            Iterator it = bootstrap.keySet().iterator();
            while(it.hasNext()){
                String key = (String)it.next();
                store.setDefault(key, bootstrap.getProperty(key));
            }
            
            this.initRootSpec();
            DDUIPlugin.this.getPluginPreferences().addPropertyChangeListener(this);
        }
        
        public INodeList getNodelist(){
            return this;
        }
        
        public ObjectSpecStringfier getEncoder(){
            if(encoder == null)
                encoder = new ObjectSpecStringfier(this.getSpecLoader(), new Base64Encoder());
            
            return encoder;
        }
        
        public Properties getBootstrapProperties(){
            return bootstrap;
        }
        
        public void initRootSpec() 
            throws CoreException
        {
            Preferences preferences = DDUIPlugin.this.getPluginPreferences();
            try {

                if (rootSpec == null) {
                    rootSpec = this.loadCreate(preferences
                            .getString(IDebuggerConstantsUI.ROOT_TYPE));
                    nodeList = loadCreate(preferences
                            .getString(IDebuggerConstantsUI.NODE_LIST_TYPE));
                    rootSpec.addChild(nodeList);
                }

                attName = preferences
                        .getString(IDDCorePluginConstants.NAME_ATTRIBUTE);
                
                /** We now initialize the root spec attributes that depend
                 * on preferences.
                 */
                rootSpec.setAttribute(IConfigurationConstants.CDWP_PORT, 
                        preferences.getString(IConfigurationConstants.CDWP_PORT));
                rootSpec.setAttribute(IConfigurationConstants.MAX_QUEUE_LENGTH,
                        preferences.getString(IConfigurationConstants.MAX_QUEUE_LENGTH));
                rootSpec.setAttribute(IConfigurationConstants.THREAD_POOL_SIZE,
                        preferences.getString(IConfigurationConstants.THREAD_POOL_SIZE));
                rootSpec.setAttribute(IConfigurationConstants.GLOBAL_AGENT_ADDRESS,
                        preferences.getString(IConfigurationConstants.GLOBAL_AGENT_ADDRESS));
                
                
            } catch (Exception ex) {
            	throwCoreExceptionWithError("Error while initializing root specification.", 
            			ex);
            }
        }
        
        private IObjectSpec loadCreate(String listSpec)
            throws SpecNotFoundException, IOException, SAXException, InstantiationException
        {
            ISpecLoader loader = this.getSpecLoader();
            String [] splitSpec = listSpec.split(IConfigurationConstants.LIST_SEPARATOR_CHAR);
            String _intfType = splitSpec[0];
            String _concType = (splitSpec.length == 2)?splitSpec[1]:null;
            
            ISpecType intfType = loader.specForName(_intfType);
            IObjectSpecType concType = loader.specForName(_concType, intfType);
            
            return concType.makeInstance();
        }

        public void rebindSpec(IObjectSpec spec) 
            throws DuplicateSymbolException, AttributeAccessException
        {
            String name = spec.getAttribute(attName);
            try{
            	/** This name has already been bound. */
                if(name2Node.containsKey(name)){
                    IObjectSpec alreadyBound = (IObjectSpec)name2Node.get(name);
                    if(alreadyBound != spec)
                        throw new DuplicateSymbolException("A node under that name already exists.");
                } 
                
                /** This node has already been bound, but under
                 * a different name (its name has changed).
                 */
                else if (node2Name.containsKey(spec)){
                	node2Name.remove(spec);
                	name2Node.put(name, spec);
                } 
                
                /** Never been bound. */
                else {
                	nodeList.addChild(spec);
                	name2Node.put(name, spec);
                }

                fireBindNodeEvent(name, spec);
                
            }catch(DuplicateSymbolException ex){
                throw ex;
            }catch(Exception ex){
                logger.error("Failed to rebind object specification to node list.", ex);
            }
        }
        
        public void unbindSpec(IObjectSpec spec) throws NoSuchSymbolException, AttributeAccessException{
            String errorMessage = "Couldn't find bound node " + spec;
            
            String name = spec.getAttribute(attName);
            if(!name2Node.containsKey(name)) throw new NoSuchSymbolException(errorMessage);
            try{
                nodeList.removeChild(spec);
                fireUnbindNodeEvent(name);
            }catch(IllegalAttributeException ex){
                throw new NoSuchSymbolException(errorMessage);
            }
        }
        
        public IObjectSpec getSpec(String name){
            return (IObjectSpec)name2Node.get(name);
        }

        public ISpecLoader getSpecLoader() {
            if (loader == null) {
            	try{
            		this.setSpecLookupPath();
            	}catch(CoreException ex){
            		throw new NestedRuntimeException("Failed to initialize the spec loader", ex);
            	}

                /** Initializes the spec loader. */
                Preferences preferences = DDUIPlugin.this.getPluginPreferences();

                /**
                 * Starts by getting the relative URL path of the specifications
                 * directory.
                 */
                String urlSuffix = preferences
                        .getString(IDebuggerConstantsUI.TOC_SUBPATH);

                /**
                 * Acquires the installation directory URL and builds the TOC
                 * relative path from there.
                 */
                Bundle bundle = DDUIPlugin.this.getBundle();
                URL urlPrefix = bundle.getEntry("/");
                String baseURL = urlPrefix.toString();
                baseURL = baseURL.endsWith("/") ? baseURL : baseURL + "/";
                String tocURL = baseURL + urlSuffix;

                /** And finally creates the loader. */
                SpecLoader theLoader = new SpecLoader(null, tocURL);
                theLoader.registerURLLister(new BundleEntryLister(),
                        BundleEntryLister.BUNDLE_URL_PROTOCOL);
                loader = theLoader;
                SpecLoader.setContextSpecLoader(theLoader);
            }

            return loader;
        }
        
        
        public IImplementationScanner getImplementationScanner()
        {
            if(scanner == null){
            	try{
            		this.setSpecLookupPath();
            	}catch(CoreException ex){
            		/** This will trigger an error message. */
            		throw new NestedRuntimeException("Failed to initialize.", ex);
            	}
                FSImplementationScanner fscanner = new FSImplementationScanner(true, this.getSpecLoader());
                fscanner.registerURLLister(new BundleEntryLister());
                scanner = fscanner;
            }
            
            return scanner;
        }
        
        private void setSpecLookupPath()
        	throws CoreException
        {
            String currentPath = System.getProperty("spec.lookup.path");
            if(currentPath != null) return;
            Preferences preferences = DDUIPlugin.this.getPluginPreferences();
            /** Acquires the installation directory URL and builds the TOC relative path from 
             * there. 
             */
            Bundle bundle = DDUIPlugin.this.getBundle();
            URL urlPrefix = bundle.getEntry("/");
            String baseURL = urlPrefix.toString();
            baseURL = baseURL.endsWith("/")?baseURL:baseURL + "/";
            String specList = preferences.getString(IDebuggerConstantsUI.SPECS_SUBPATH_LIST);

            /** Now retrieves the spec lookup path urls. */
            StringBuffer fullSpecList = new StringBuffer();
            for(String specPath : specList.split(IConfigurationConstants.LIST_SEPARATOR_CHAR)){
                URI uri;
                try{
                    uri = new URI(specPath);
                }catch(URISyntaxException ex){
                    logger.error("Specification lookup paths must valid URIs. Element " + specPath + "has been ignored." , ex);
                    continue;
                }

                /** Is this URI a URL? */
                if(uri.getScheme() != null)
                    fullSpecList.append(specPath);
                else{
                	URL specPathURL = bundle.getEntry(specPath);
                	if(specPathURL == null)
                		throwCoreExceptionWithError("Cannot find the specifications directory. Check your configuration.", 
                				null);
                	
                    fullSpecList.append(bundle.getEntry(specPath).toString());
                }
                
                fullSpecList.append(IConfigurationConstants.LIST_SEPARATOR_CHAR);
            }
            
            String theList = "";
            /** Trims the last separator. */
            if(fullSpecList.length() >= 1) theList = fullSpecList.substring(0, fullSpecList.length()-1);
            
            System.setProperty("spec.lookup.path", theList);
        }
        
        public IObjectSpec getRootSpec(){
            return rootSpec;
        }
        
        private void fireBindNodeEvent(String name, IObjectSpec theSpec){
//            for(IConfigurationUser user : this.getLoadExtenders())
//                user.bindConfiguration(theSpec, name);
            DDCorePlugin.getDefault().getNodeManager().bindConfiguration(theSpec, name);
        }
        
        private void fireUnbindNodeEvent(String name){
//            for(IConfigurationUser user: this.getLoadExtenders())
//                user.unbindConfiguration(name);
            DDCorePlugin.getDefault().getNodeManager().unbindConfiguration(name);
        }

        public void propertyChange(PropertyChangeEvent event) {
            try{
                this.getRootSpec().setAttribute(event.getProperty(), (String)event.getNewValue());
            }catch(IllegalAttributeException ex){
                // It's OK.
            }catch(InvalidAttributeValueException ex){
                logger.error("Invalid value for property " + event.getProperty(), ex);
            }catch(ClassCastException ex){
                // OK as well. 
            }
        }
        
//        private List<IConfigurationUser> getLoadExtenders(){
//            if(extenders != null) return extenders;
//            extenders = new ArrayList<IConfigurationUser>();
//            IExtensionRegistry registry = Platform.getExtensionRegistry();
//            try {
//                IExtensionPoint extensionPoint = registry
//                        .getExtensionPoint(IDebuggerConstantsUI.CONFIGURATION_USERS_EP);
//                IConfigurationElement[] extenderArray = extensionPoint
//                        .getConfigurationElements();
//                for (IConfigurationElement extender : extenderArray) {
//                    try {
//                        extenders.add((IConfigurationUser) extender
//                                        .createExecutableExtension(IDebuggerConstantsUI.CLASS_PROPERTY));
//                    } catch (Exception ex) {
//                        logger.error("Error while attempting to instantiate extension.", ex);
//                    }
//                }
//            } catch (InvalidRegistryObjectException ex) {
//                logger.error("A registry object has become invalid. Error while processing extensions.", ex);
//            }
//            
//            return extenders;
//        }
    }
    
    private void configureLogManager() {
        Bundle bundle = DDUIPlugin.this.getBundle();
        Preferences prefs = DDUIPlugin.this.getPluginPreferences();
        URL url = bundle.getEntry(
                "/" + prefs.getString(IDebuggerConstantsUI.LOG_PROPERTIES_FILE));
        InputStream propertiesInputStream = null;
        try{
            if(url != null) propertiesInputStream = url.openStream();
        }catch(IOException ex) { }
        Properties props = new Properties();
        
        try{
            if (propertiesInputStream != null) {
                props.load(propertiesInputStream);
                propertiesInputStream.close();
            }
        }catch(IOException ex){
            DDUIPlugin.this.getLog().log(new Status(IStatus.ERROR, bundle.getSymbolicName(), IStatus.ERROR, 
                    "Failed to read log configuration file " + IDebuggerConstantsUI.LOG_PROPERTIES_FILE + ". Will" +
                    " attempt to use default configurations.", ex));
        }
        this.logManager = new PluginLogManager(DDUIPlugin.this, props);
            
        logger = this.getLogManager().getLogger(DDUIPlugin.class);
        
        if(logger.isDebugEnabled()) logger.debug("Logger is online.");
    }
    
    public static void throwCoreExceptionWithError(String reason, Throwable t)
    	throws CoreException
    {
        throw new CoreException(new Status(IStatus.ERROR, 
                plugin.getBundle().getSymbolicName(), IStatus.ERROR, reason, t));
    }
    
}
