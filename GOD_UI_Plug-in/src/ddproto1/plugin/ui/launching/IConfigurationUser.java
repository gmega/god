/*
 * Created on Nov 26, 2005
 * 
 * file: IConfigurationChangeListener.java
 */
package ddproto1.plugin.ui.launching;

import ddproto1.configurator.IObjectSpec;

public interface IConfigurationUser {
    /**
     * Binds a configuration for a node into a configuration user. The configuration
     * user should NOT modify the IObjectSpec passed as parameter, even though this 
     * rule will not be enforced by the implementation.
     * 
     * This method might be called multiple times, as the bound configuration changes.
     * 
     * @param newSpec spec for the bound node.
     */
    public void bindConfiguration(IObjectSpec newSpec, String nodeName);
    public void unbindConfiguration(String nodeName);
}