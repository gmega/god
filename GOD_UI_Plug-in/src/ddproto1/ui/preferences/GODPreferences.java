package ddproto1.ui.preferences;

import java.util.Properties;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.plugin.ui.DDUIPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class GODPreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IConfigurationConstants {

	public GODPreferences() {
		super(GRID);
		setPreferenceStore(DDUIPlugin.getDefault().getPreferenceStore());
		setDescription("The Global Online Debugger Preference Page");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
        Properties props = DDUIPlugin.getDefault().getPreferencePagesSpec();
        
        for(Object propKey : props.keySet()){
            String _propKey = (String)propKey;
            addField(new StringFieldEditor(_propKey, props
                    .getProperty(_propKey), getFieldEditorParent()));
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}