/*
 * Created on Sep 10, 2005
 * 
 * file: ComponentConfiguratorTab.java
 */
package ddproto1.plugin.ui.launching;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import ddproto1.configurator.IAttribute;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.ObjectSpecStringfier;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.plugin.core.IDDCorePluginConstants;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.IConfigurationManager;
import ddproto1.plugin.ui.IDebuggerConstantsUI;
import ddproto1.plugin.ui.launching.ConfigurationPanel;


public class ComponentConfiguratorTab extends AbstractLaunchConfigurationTab implements IAttributeChangeListener{

    private ConfigurationPanel panel;
    private Logger logger = DDUIPlugin.getDefault().getLogManager().getLogger(ComponentConfiguratorTab.class);
    
    public void createControl(Composite parent) {
        panel = new ConfigurationPanel(parent, SWT.NONE);
        panel.addAttributeChangeListener(this);
        this.setControl(panel);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        DDUIPlugin plugin = DDUIPlugin.getDefault();
        IConfigurationManager icm = plugin.getConfigurationManager();
        /** Retrieves the default specification root type from the 
         * plug-in preferences.
         */
        Preferences preferences = plugin.getPluginPreferences();
        String rootType = preferences.getString(IDebuggerConstantsUI.NODE_CONFIG_TYPE);
        String nameAtt  = preferences.getString(IDDCorePluginConstants.NAME_ATTRIBUTE);
        String defaultName = configuration.getName();
        IImplementationScanner iscanner = plugin.getConfigurationManager()
                .getImplementationScanner();
        try{
            Iterable<IObjectSpecType> defSpecs = iscanner.retrieveImplementationsOf(rootType);
            Iterator<IObjectSpecType> specIt = defSpecs.iterator();

            if(!specIt.hasNext()) return;
            
            /** Retrieves the first implementation of the root type. */
            IObjectSpecType ostype = specIt.next();
            IObjectSpec os = ostype.makeInstance();
            os.setAttribute(nameAtt, defaultName);
            configuration.setAttribute(IDebuggerConstantsUI.ROOT_ATTRIBUTE, 
                    icm.getEncoder().makeFromObjectSpec(os));
            configuration.setAttribute(IDDCorePluginConstants.NAME_ATTRIBUTE, 
                    defaultName);
            icm.getNodelist().rebindSpec(os);
                        
        }catch(Exception ex){
            plugin.getLog().log(new Status(Status.ERROR, DDUIPlugin.getDefault().getBundle().getSymbolicName(),
                            IDebuggerConstantsUI.STATS_NO_ROOT_IMPL,
                            "Failed to load root implementation or to initialize the spec instance"
                                    + IDebuggerConstantsUI.NODE_CONFIG_TYPE
                                    + ".", ex));
        }
        
    }

    public void initializeFrom(ILaunchConfiguration configuration) {
        try{
            String root = configuration.getAttribute(
                    IDebuggerConstantsUI.ROOT_ATTRIBUTE, "");
            String name = configuration.getAttribute(IDDCorePluginConstants.NAME_ATTRIBUTE, "");
            
            if(root.equals("")){
                logger.error("Restored configuration does not contain a root attribute.");
                return;
            }

            IConfigurationManager icm = DDUIPlugin.getDefault().getConfigurationManager();
            IObjectSpec theChild = icm.getNodelist().getSpec(name); 
            
            if(theChild == null){
                theChild = icm.getEncoder().restoreFromString(root);
                icm.getNodelist().rebindSpec(theChild);
            }
            
            panel.setObjectSpecRoot(theChild);
            
            this.setDirty(false);
            
        }catch(Exception ex){ 
            String errorMessage = "Failed to restore configuration. Check the error log.";
            logger.error(errorMessage, ex);
            this.setErrorMessage(errorMessage);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        DDUIPlugin plugin = DDUIPlugin.getDefault();
        IConfigurationManager icm = plugin.getConfigurationManager();
        Preferences prefs = plugin.getPluginPreferences();
        String attName = prefs.getString(IDDCorePluginConstants.NAME_ATTRIBUTE);
        
        IObjectSpec spec = panel.getObjectSpecRoot();
        if(spec == null){
            logger.error("The in-memory configurations have not been correctly initialized. Cannot restore configurations.");
            return;
        }
       
        try{
            configuration.setAttribute(IDDCorePluginConstants.NAME_ATTRIBUTE, spec.getAttribute(attName));
        }catch(AttributeAccessException ex){
            String error = "Cannot apply name attribute - unnamed configuration."; 
            logger.error(error, ex);
            this.setErrorMessage(error);
        }
        configuration.setAttribute(IDebuggerConstantsUI.ROOT_ATTRIBUTE, icm.getEncoder().makeFromObjectSpec(spec));

        try{
            icm.getNodelist().rebindSpec(spec);
        }catch(Exception ex){
            String error = "Failed to rebind node specification into configuration manager.";
            logger.error(error, ex);
            this.setErrorMessage(error);
        }
    }

    public String getName() {
        return "Node Configuration";
    }
    
    public void dispose(){ super.dispose(); }

    public void attributeChanged(IAttribute attribute, String value) {
        this.setDirty(true);
        this.updateLaunchConfigurationDialog();
    }
}
