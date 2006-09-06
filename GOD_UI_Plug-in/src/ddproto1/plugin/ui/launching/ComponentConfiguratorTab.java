/*
 * Created on Sep 10, 2005
 * 
 * file: ComponentConfiguratorTab.java
 */
package ddproto1.plugin.ui.launching;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IAttribute;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.plugin.IConfigurationManager;
import ddproto1.configurator.plugin.IImplementationScanner;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.UIDebuggerConstants;


public class ComponentConfiguratorTab extends AbstractLaunchConfigurationTab implements IAttributeChangeListener{

    private ConfigurationPanel panel;
    private Logger logger = DDUIPlugin.getDefault().getLogManager().getLogger(ComponentConfiguratorTab.class);
    
    public void createControl(Composite parent) {
        panel = new ConfigurationPanel(parent, SWT.NONE);
        panel.addAttributeChangeListener(this);
        this.setControl(panel);
    }

    /**
     * Creates a new empty configuration. 
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        GODBasePlugin gbPlugin = GODBasePlugin.getDefault();
        IConfigurationManager icm = gbPlugin.getConfigurationManager();
        /** Creates new node specification. The default specification
         * will be of the first concrete type available.
         */
        String defaultName = configuration.getName();
        IImplementationScanner iscanner = icm.getImplementationScanner();
        try{
            Iterable<IObjectSpecType> defSpecs = iscanner.retrieveImplementationsOf(IConfigurationConstants.NODE);
            Iterator<IObjectSpecType> specIt = defSpecs.iterator();

            if(!specIt.hasNext()) return;
            
            /** Retrieves the first implementation of the root type. */
            IObjectSpecType ostype = specIt.next();
            IObjectSpec os = ostype.makeInstance();
            
            /** Serialize the instance into a string, then stores the blob 
             * into the configuration working copy, together with the name
             * of the configuration. */
            os.setAttribute(IConfigurationConstants.NAME_ATTRIB, defaultName);
            configuration.setAttribute(UIDebuggerConstants.ROOT_ATTRIBUTE, 
                    icm.getEncoder().makeFromObjectSpec(os));
            configuration.setAttribute(IConfigurationConstants.NAME_ATTRIBUTE, 
                    defaultName);
            
            /** Adds the in-memory configuration to the base plugin configuration
             * list. Having the in-memory configuration at the base plug-in is 
             * sort of a hack, it is a way I found to connect the global agent
             * config with the node configs, but I should come up with a better
             * decomposition.
             */
            icm.getNodelist().rebindSpec(os);
                        
        }catch(Exception ex){
            logger.error("Failed to load root implementation or to initialize the spec instance "
                                    + IConfigurationConstants.NODE
                                    + ".", ex);
        }
        
    }

    /**
     * Restores a previously created configuration.
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        try{
            String root = configuration.getAttribute(
                    UIDebuggerConstants.ROOT_ATTRIBUTE, "");
            String name = configuration.getAttribute(IConfigurationConstants.NAME_ATTRIBUTE, "");
            
            if(root.equals("")){
                logger.error("Restored configuration does not contain a root attribute.");
                return;
            }

            IConfigurationManager icm = GODBasePlugin.getDefault().getConfigurationManager();
            IObjectSpec theChild = icm.getNodelist().getSpec(name); 
            
            if(theChild == null){
                theChild = icm.getDecoder().restoreFromString(root);
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
        GODBasePlugin plugin = GODBasePlugin.getDefault();
        IConfigurationManager icm = plugin.getConfigurationManager();
        Preferences prefs = plugin.getPluginPreferences();
        
        IObjectSpec spec = panel.getObjectSpecRoot();
        if(spec == null){
            logger.error("The in-memory configurations have not been correctly initialized. Cannot restore configurations.");
            return;
        }
       
        try{
            configuration.setAttribute(IConfigurationConstants.NAME_ATTRIBUTE, spec.getAttribute(IConfigurationConstants.NAME_ATTRIBUTE));
        }catch(AttributeAccessException ex){
            String error = "Cannot apply name attribute - unnamed configuration."; 
            logger.error(error, ex);
            this.setErrorMessage(error);
        }
        configuration.setAttribute(UIDebuggerConstants.ROOT_ATTRIBUTE, icm.getEncoder().makeFromObjectSpec(spec));

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
