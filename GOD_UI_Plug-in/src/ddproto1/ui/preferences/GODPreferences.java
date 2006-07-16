package ddproto1.ui.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.IDebuggerConstantsUI;

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
		addField(
			new StringFieldEditor(IDebuggerConstantsUI.NODE_CONFIG_TYPE, "Roo&t configuration attribute type:", getFieldEditorParent()));
        addField(
                new StringFieldEditor(IDebuggerConstantsUI.TOC_SUBPATH, "&Specification directory relative path list:", getFieldEditorParent()));
        addField(
                new StringFieldEditor(IDebuggerConstantsUI.SPECS_SUBPATH_LIST, "&TOC relative path:", getFieldEditorParent()));
        
        addField(
                new StringFieldEditor(THREAD_POOL_SIZE, "&Maximum thread pool size:", getFieldEditorParent()));
        addField(
                new StringFieldEditor(CDWP_PORT, "CDWP &Port:", getFieldEditorParent()));
        addField(
                new StringFieldEditor(MAX_QUEUE_LENGTH, "Maximum &connection queue size:", getFieldEditorParent()));
        addField(
                new StringFieldEditor(GLOBAL_AGENT_ADDRESS, "&Global agent IP address:", getFieldEditorParent()));

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}