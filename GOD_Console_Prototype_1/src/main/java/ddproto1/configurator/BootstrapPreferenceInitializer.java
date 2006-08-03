/*
 * Created on Jul 17, 2006
 * 
 * file: BootstrapPreferenceInitializer.java
 */
package ddproto1.configurator;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.NestedRuntimeException;

public class BootstrapPreferenceInitializer extends AbstractPreferenceInitializer{

    @Override
    public void initializeDefaultPreferences() {
        URL bstrapURL =
           GODBasePlugin.getDefault().getBundle().getEntry(
                "/" + IConfigurationConstants.BOOTSTRAP_PROPERTIES_FILE); 
        
        if(bstrapURL == null)
            throw new RuntimeException("Error while attempting to load bootstrap properties.");
        
        Properties props = new Properties();
        
        try{
            props.load(bstrapURL.openStream());
        }catch(IOException ex){
            throw new NestedRuntimeException(ex);
        }
        
        Preferences prefs = GODBasePlugin.getDefault().getPluginPreferences();
        
        for(Object key : props.keySet()){
            String _key = (String)key;
            prefs.setDefault(_key, props.getProperty(_key));
        }
    }

}
