package ddproto1.ui.preferences;

import java.util.Properties;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.IDebuggerConstantsUI;

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
        DDUIPlugin plugin = DDUIPlugin.getDefault();
        
		IPreferenceStore store = plugin.getPreferenceStore();
        Properties bootstrap = plugin.getConfigurationManager().getBootstrapProperties();
        
        /** Applies the bootstrap properties. */
        String tocSubpath = bootstrap.getProperty(IDebuggerConstantsUI.TOC_SUBPATH);
		store.setDefault(IDebuggerConstantsUI.TOC_SUBPATH, tocSubpath);
		store.setDefault(IDebuggerConstantsUI.SPECS_SUBPATH_LIST, tocSubpath);
        
        store.setDefault(GLOBAL_AGENT_ADDRESS, IDebuggerConstantsUI.DEFAULT_GA_ADDRESS);
        store.setDefault(CDWP_PORT, IDebuggerConstantsUI.DEFAULT_CDWP_PORT);
        store.setDefault(MAX_QUEUE_LENGTH, IDebuggerConstantsUI.DEFAULT_CONN_QUEUE);
        store.setDefault(THREAD_POOL_SIZE, IDebuggerConstantsUI.DEFAULT_THREAD_POOL);
	}

}
