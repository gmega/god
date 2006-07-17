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

import ddproto1.GODBasePlugin;
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
