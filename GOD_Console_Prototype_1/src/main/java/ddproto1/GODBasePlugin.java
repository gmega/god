/*
 * Created on Feb 6, 2006
 * 
 * file: GODBasePlugin.java
 */
package ddproto1;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.debug.core.DebugException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

import com.tools.logging.PluginLogManager;

import ddproto1.configurator.BundleEntryLister;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecDecoder;
import ddproto1.configurator.IObjectSpecEncoder;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.ISpecLoader;
import ddproto1.configurator.ISpecType;
import ddproto1.configurator.ObjectSpecStringfier;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.SpecNotFoundException;
import ddproto1.configurator.StandardServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.plugin.FSImplementationScanner;
import ddproto1.configurator.plugin.IConfigurationManager;
import ddproto1.configurator.plugin.IImplementationScanner;
import ddproto1.configurator.plugin.INodeList;
import ddproto1.debugger.managing.GlobalAgent;
import ddproto1.debugger.managing.identification.FIFOGUIDManager;
import ddproto1.debugger.managing.identification.IGUIDManager;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.launcher.IApplicationLauncher;
import ddproto1.launcher.procserver.IProcessServerManager;
import ddproto1.launcher.procserver.ProcessServerManager;
import ddproto1.launcher.procserver.PyExpectSSHExecutor;
import ddproto1.util.Base64Encoder;
import ddproto1.util.ILogManager;
import ddproto1.util.IServiceLifecycle;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;

/**
 * This plugin class will perform part of the initialization routines that used to be
 * performed by ddproto1.Main and ddproto1.ConsoleDebugger. Most of the initialization
 * code has been moved to the UI and Core plugins, so this class does the leftover 
 * stuff.
 * 
 * Update (06/2006): Not anymore. This class absorbed many of the UI and Core plug-in 
 * classes functionality. 
 *  
 * @author giuliano
 *
 */
public class GODBasePlugin extends Plugin {
    
    private volatile static GODBasePlugin pluginInstance = null;

    /** Volatility is to ensure publication safety. These classes
     * are all thread-safe, so this should be enough. */
    private volatile PluginLogManager plManager;
    private volatile Logger defaultLogger;
    private volatile ProcessServerManager psManager;
    
    private volatile IGUIDManager processGUIDManager;
    private volatile IGUIDManager debuggeeIDManager;
    
    private final ConfigManagerImpl cmi = new ConfigManagerImpl();
    
    private GlobalAgent gaSingleton;
            
    public GODBasePlugin(){ }
    
    public static GODBasePlugin getDefault(){
        return pluginInstance;
    }
    
    /* (non-Javadoc)1
     * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        assert pluginInstance == null;
        pluginInstance = this;
        super.start(context);
        initializeLoggerManager();
        initializeIDGenerators();
        startBasicServices();
        cmi.start();
        setGASingleton(new GlobalAgent(cmi.getRootSpec()));
    }
    
    private void setGASingleton(GlobalAgent singleton){
        gaSingleton = singleton;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        getProcessServerManager().stop();
    }
    
    protected void initializeLoggerManager(){
        
        /**
         * Starts by creating and registering a log manager for this plugin with
         * the Logging Plugin.
         */
        Bundle bundle = this.getBundle();
        
        URL url = bundle.getEntry(
                "/" + (getPluginPreferences().getString(IConfigurationConstants.LOG_PROPERTIES_FILE)));
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
            this.getLog().log(new Status(IStatus.ERROR, bundle.getSymbolicName(), IStatus.ERROR, 
                    "Failed to read log configuration file " + IConfigurationConstants.LOG_PROPERTIES_FILE + ". Will" +
                    " attempt to use default configurations.", ex));
        }
        this.plManager = new PluginLogManager(this, props);
        this.defaultLogger = plManager.getLogger(GODBasePlugin.class);
        
        /** Now initializes the legacy interface. We're using simple
         * implementations that treat print and println the same way. The
         * correct way to do this would be to emulate print behavior with 
         * a string buffer, but that would be too much trouble to be worth it.
         * 
         * */
        IMessageBox warning = new IMessageBox(){
            public void println(String s) { defaultLogger.warn(s); }
            public void print  (String s) { defaultLogger.warn(s); }
        };
        
        IMessageBox error = new IMessageBox(){
            public void println(String s) { defaultLogger.error(s); }
            public void print  (String s) { defaultLogger.error(s); }
        };
        
        IMessageBox info = new IMessageBox(){
            public void println(String s) { defaultLogger.info(s); }
            public void print(String s) { defaultLogger.info(s); }
        };
        
        IMessageBox debug = new IMessageBox(){
            public void println(String s) { defaultLogger.debug(s); }
            public void print(String s) { defaultLogger.debug(s); }
        };
        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setStandardOutput(info);
        mh.setErrorOutput(error);
        mh.setWarningOutput(warning);
        mh.setDebugOutput(debug);

        /** Now we initialize the logging manager interface. */
        mh.setLogManagerDelegate(new ILogManager(){
            public Logger getLogger(Class c) { return plManager.getLogger(c); }
            public Logger getLogger(String name) { return plManager.getLogger(name); }
        });
        
        defaultLogger.info("Logger service has been configured successfully.");        
    }
    
    private void startBasicServices() throws CoreException {

        try {
            Lookup.serviceRegistry().start();
            Lookup.serviceRegistry().register(
                    IConfigurationConstants.SERVICE_LOCATOR,
                    new StandardServiceLocator());
        } catch (Exception ex) {
            throwCoreExceptionWithError(
                    "Failed to start the lookup service. Plug-in will not operate correctly.",
                    ex);
        }
    }
    
    protected void initializeProcserverManager(ProcessServerManager psManager)
    {
        try{
            Preferences prefs = 
                GODBasePlugin.this.getPluginPreferences();
            psManager.setAttribute(IConfigurationConstants.CALLBACK_OBJECT_PATH,
                    prefs.getString(IConfigurationConstants.CALLBACK_OBJECT_PATH));
            psManager.setAttribute(IConfigurationConstants.RMI_REGISTRY_PORT,
                    prefs.getString(IConfigurationConstants.RMI_REGISTRY_PORT));
        }catch(Exception ex){
            defaultLogger.error("Failed to start the process server manager. " +
                    "Launching of remote processes is disabled.", ex);
        }
    }
        
    public synchronized IGUIDManager processGUIDManager(){
        return processGUIDManager;
    }
    
    public synchronized IGUIDManager debuggeeGUIDManager(){
        return debuggeeIDManager;
    }
    
    protected void initializeIDGenerators(){
        try{
            
            /** Process server managers use Integers as identifiers, therefore 
             * they can couple with Integer.MAX_VALUE - Integer.MIN_VALUE 
             * different IDs. We'll restrict these further though, since reusable
             * ids are expensive to keep.                                */
            this.processGUIDManager = new FIFOGUIDManager(20000, 100);
            
            /** Debuggees use bytes as IDs. This is somewhat restrictive and it'd be
             * though to change (the actual coupling with this value is disseminated
             * in subtle ways throughout the debugger). */
            this.debuggeeIDManager = new FIFOGUIDManager(255, 10);
            
        }catch(InvalidAttributeValueException ex){
            throw new InternalError();
        }

    }
    
    public synchronized IProcessServerManager getProcessServerManager(){
        return getProcessServerManagerInternal();
    }
    
    private synchronized ProcessServerManager getProcessServerManagerInternal(){
        if(psManager == null)
            psManager = new ProcessServerManager();
        
        if(psManager.currentState() == IServiceLifecycle.STOPPED)
            initializeProcserverManager(psManager);
        
        return psManager;
    }
    
    public IConfigurationManager getConfigurationManager(){
        return cmi;
    }

    public GlobalAgent getGlobalAgent() {
        return gaSingleton;
    }
    
    /**
     * This class exposes the main interfaces for configuration 
     * of the core plugin.
     * 
     * This class is thread-safe, but it is highly non-concurrent. We were mostly
     * worried with not having the occasional multi-thread access corrupting internal 
     * state and with memory synchronization effects. 
     * 
     * @author giuliano
     */
    private class ConfigManagerImpl implements IConfigurationManager, INodeList, IPropertyChangeListener{

        private ISpecLoader             loader;
        private IImplementationScanner  scanner;
        
        private IObjectSpec             rootSpec;
        private IObjectSpec             nodeList;
        
        private String                  attName;
        
        private ObjectSpecStringfier    encoder;
               
        private final BidiMap name2Node = new DualHashBidiMap();
        private final BidiMap node2Name = name2Node.inverseBidiMap();
        
        public synchronized void start()
            throws CoreException
        {
            Preferences preferences = GODBasePlugin.this.getPluginPreferences();
            this.initRootSpec();
            preferences.addPropertyChangeListener(this);
        }
        
        public synchronized INodeList getNodelist(){
            return this;
        }
        
        public IObjectSpecEncoder getEncoder() {
            return getCodec();
        }

        public IObjectSpecDecoder getDecoder() {
            return getCodec();
        }

        private ObjectSpecStringfier getCodec() {

            if (encoder == null)
                encoder = new ObjectSpecStringfier(this.getSpecLoader(),
                        new Base64Encoder());

            return encoder;
        }

        public synchronized void initRootSpec() 
            throws CoreException
        {
            Preferences preferences = GODBasePlugin.this.getPluginPreferences();
            try {

                if (rootSpec == null) {
                    rootSpec = this.loadCreate(IConfigurationConstants.ROOT_TYPE);
                    nodeList = loadCreate(IConfigurationConstants.NODE_LIST);
                    rootSpec.addChild(nodeList);
                }

                attName = IConfigurationConstants.NAME_ATTRIBUTE;
                
                /** We now initialize the root spec attributes that depend
                 * on preferences.
                 */
                for(String attribute : rootSpec.getAttributeKeys()){
                    if(!preferences.contains(attribute))
                        defaultLogger.error("Can't configure property " + attribute + " from plug-in preferences.");
                    rootSpec.setAttribute(attribute, preferences.getString(attribute));
                }
                
                if(!rootSpec.validate())
                    throwCoreExceptionWithError("The plug-in could not be properly configured and therefore will not be started.", null);
                
            } catch (Exception ex) {
                throwCoreExceptionWithError("Error while initializing root specification.", 
                        ex);
            }
        }
        
        private synchronized IObjectSpec loadCreate(String listSpec)
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

        public synchronized void rebindSpec(IObjectSpec spec) 
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

            }catch(DuplicateSymbolException ex){
                throw ex;
            }catch(Exception ex){
                defaultLogger.error("Failed to rebind object specification to node list.", ex);
            }
        }
        
        public synchronized void unbindSpec(IObjectSpec spec) throws NoSuchSymbolException, AttributeAccessException{
            String errorMessage = "Couldn't find bound node " + spec;
            
            String name = spec.getAttribute(attName);
            if(!name2Node.containsKey(name)) throw new NoSuchSymbolException(errorMessage);
            try{
                nodeList.removeChild(spec);
            }catch(IllegalAttributeException ex){
                throw new NoSuchSymbolException(errorMessage);
            }
        }
        
        public synchronized IObjectSpec getSpec(String name){
            return (IObjectSpec)name2Node.get(name);
        }

        public synchronized ISpecLoader getSpecLoader() {
            if (loader == null) {
                try{
                    this.setSpecLookupPath();
                }catch(CoreException ex){
                    throw new NestedRuntimeException("Failed to initialize the spec loader", ex);
                }

                /** Initializes the spec loader. */
                Preferences preferences = GODBasePlugin.this.getPluginPreferences();

                /**
                 * Starts by getting the relative URL path of the specifications
                 * directory.
                 */
                String urlSuffix = preferences
                        .getString(IConfigurationConstants.TOC_DIR);

                /**
                 * Acquires the installation directory URL and builds the TOC
                 * relative path from there.
                 */
                Bundle bundle = GODBasePlugin.this.getBundle();
                URL urlPrefix = bundle.getEntry("/");
                String baseURL = urlPrefix.toString();
                baseURL = baseURL.endsWith("/") ? baseURL : baseURL + "/";
                String tocURL = baseURL + urlSuffix;

                /** And finally creates the loader. */
                SpecLoader theLoader = new SpecLoader(null, tocURL);
                theLoader.registerURLLister(new BundleEntryLister(getBundle()));
                loader = theLoader;
                SpecLoader.setContextSpecLoader(theLoader);
            }

            return loader;
        }
        
        
        public synchronized IImplementationScanner getImplementationScanner()
        {
            if(scanner == null){
                try{
                    this.setSpecLookupPath();
                }catch(CoreException ex){
                    /** This will trigger an error message. */
                    throw new NestedRuntimeException("Failed to initialize.", ex);
                }
                FSImplementationScanner fscanner = new FSImplementationScanner(true, this.getSpecLoader());
                fscanner.registerURLLister(new BundleEntryLister(getBundle()));
                scanner = fscanner;
            }
            
            return scanner;
        }
        
        private synchronized void setSpecLookupPath()
            throws CoreException
        {
            String currentPath = System.getProperty("spec.lookup.path");
            if(currentPath != null) return;
            Preferences preferences = GODBasePlugin.this.getPluginPreferences();
            /** Acquires the installation directory URL and builds the TOC relative path from 
             * there. 
             */
            Bundle bundle = GODBasePlugin.this.getBundle();
            URL urlPrefix = bundle.getEntry("/");
            String baseURL = urlPrefix.toString();
            baseURL = baseURL.endsWith("/")?baseURL:baseURL + "/";
            String specList = preferences.getString(IConfigurationConstants.SPECS_DIR);

            /** Now retrieves the spec lookup path urls. */
            StringBuffer fullSpecList = new StringBuffer();
            for(String specPath : specList.split(IConfigurationConstants.LIST_SEPARATOR_CHAR)){
                URI uri;
                try{
                    uri = new URI(specPath);
                }catch(URISyntaxException ex){
                    defaultLogger.error("Specification lookup paths must valid URIs. Element " + specPath + "has been ignored." , ex);
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
        
        public synchronized IObjectSpec getRootSpec(){
            return rootSpec;
        }

        public synchronized void propertyChange(PropertyChangeEvent event) {
            try{
                String key = event.getProperty();
                String value = (String)event.getNewValue();
                this.getRootSpec().setAttribute(key, value);
                
                ProcessServerManager pManager = getProcessServerManagerInternal();
                GlobalAgent ga = getGlobalAgent();
                
                if(pManager.getAttributeKeys().contains(key))
                    pManager.setAttribute(key, value);
                if(ga.getAttributeKeys().contains(key))
                    ga.setAttribute(key, value);
                
            }catch(IllegalAttributeException ex){
                defaultLogger.error("Unrecognized property " + event.getProperty(), ex);
            }catch(InvalidAttributeValueException ex){
                defaultLogger.error("Invalid value for property " + event.getProperty(), ex);
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
    
    public static void throwCoreExceptionWithError(String reason, Throwable t)
            throws CoreException {
        throw new CoreException(new Status(IStatus.ERROR, pluginInstance
                .getBundle().getSymbolicName(), IStatus.ERROR, reason, t));
    }

    public static void throwDebugException(String exception)
            throws DebugException {
        throwDebugExceptionWithError(exception, null);
    }

    public static void throwDebugExceptionWithError(String reason, Throwable t)
            throws DebugException {
        throw debugExceptionWithError(reason, t);
    }
    
    public static void throwDebugExceptionWithErrorAndStatus(String reason, Throwable t, int status)
        throws DebugException
    {
        throw debugExceptionWithErrorAndStatus(reason, t, status);
    }
    
    public static DebugException debugExceptionWithError(String reason, Throwable t){
        return debugExceptionWithErrorAndStatus(reason, t, IStatus.ERROR);
    }

    public static DebugException debugExceptionWithErrorAndStatus(String reason, Throwable t, int status){
        return new DebugException(new Status(IStatus.ERROR, pluginInstance
                .getBundle().getSymbolicName(), status, reason, t));
    }

}
