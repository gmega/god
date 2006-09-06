/*
 * Created on Oct 20, 2005
 * 
 * file: DDUIPlugin.java
 */
package ddproto1.plugin.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.tools.logging.PluginLogManager;

public class DDUIPlugin extends AbstractUIPlugin{
    
    //The shared instance.
    protected static DDUIPlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;
    //Logger (lazily initialized) 
    private Logger logger;
    //Logger manager.
    private PluginLogManager logManager;
    
    private Properties fPreferencePages;

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
    
    public synchronized Properties getPreferencePagesSpec(){
        if(fPreferencePages == null){
            Preferences prefs = getPluginPreferences();
            String prefPageEntry = prefs.getString(
                    UIDebuggerConstants.PREFERENCE_PAGES_FILE);
            fPreferencePages = 
                loadFromRoot(prefPageEntry, "preference pages");
        }
        
        return fPreferencePages;
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
        if(plugin == null)
            return null;
        return AbstractUIPlugin.imageDescriptorFromPlugin(plugin.getBundle().getSymbolicName(), path);
    }
    
    private void configureLogManager() {
        Bundle bundle = DDUIPlugin.this.getBundle();
        Preferences prefs = DDUIPlugin.this.getPluginPreferences();
        URL url = bundle.getEntry(
                "/" + prefs.getString(UIDebuggerConstants.LOG_PROPERTIES_FILE));
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
                    "Failed to read log configuration file " + UIDebuggerConstants.LOG_PROPERTIES_FILE + ". Will" +
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
    
    public static void logError(String error, Throwable t){
        DDUIPlugin.getDefault().getLog().log(
                new Status(IStatus.ERROR,
                           DDUIPlugin.getDefault().getBundle().getSymbolicName(),
                           IStatus.ERROR,
                               error,
                            t));
    }
    
    public static Properties loadFromRoot(String propfileName, String description){
        Bundle bundle = DDUIPlugin.getDefault().getBundle();
        URL fileURL = bundle.getEntry("/" + propfileName);
        if(fileURL == null){
            logError("Could not locate " + description + " " + propfileName, null);
            return null;
        }
        
        Properties properties = new Properties();
        try{
            properties.load(fileURL.openStream());
        }catch(Exception ex){
            DDUIPlugin.logError("Error while loading bootstrap property file.", ex);
        }
        
        return properties;
    }
}
