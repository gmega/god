package ddproto1.ui.preferences;

import java.util.Properties;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.UIDebuggerConstants;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer implements IConfigurationConstants {
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
        loadBootstrapProperties();
        getPropertiesFromBasePlugin();
	}
    
    private void loadBootstrapProperties(){
        Properties bStrap = 
            DDUIPlugin.loadFromRoot(
                UIDebuggerConstants.BOOTSTRAP_PROPERTIES_FILE,
                "bootstrap properties file");
        
        Preferences prefs = DDUIPlugin.getDefault().getPluginPreferences();
        
        for(Object key : bStrap.keySet()){
            String value = (String)bStrap.get(key);
            prefs.setDefault((String)key, value);
        }
    }
    
    private void getPropertiesFromBasePlugin(){
        Properties props = DDUIPlugin.getDefault().getPreferencePagesSpec();
        Preferences basePrefs = GODBasePlugin.getDefault().getPluginPreferences();
        Preferences ourPrefs = DDUIPlugin.getDefault().getPluginPreferences();
        for(Object propKey : props.keySet()){
            String key = (String)propKey;
            String baseValue = basePrefs.getString(key);
            if(baseValue.equals("")) continue;
            ourPrefs.setDefault(key, baseValue);
        }
    }
}
