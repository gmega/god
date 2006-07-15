/*
 * Created on Oct 21, 2005
 * 
 * file: IConfigurationManager.java
 */
package ddproto1.plugin.ui;

import java.util.Properties;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.ISpecLoader;
import ddproto1.configurator.ObjectSpecStringfier;
import ddproto1.plugin.ui.launching.IImplementationScanner;

public interface IConfigurationManager {

    public INodeList getNodelist();

    public ObjectSpecStringfier getEncoder();
    
    public ISpecLoader getSpecLoader();

    public IImplementationScanner getImplementationScanner();

    public Properties getBootstrapProperties();
    
    public IObjectSpec getRootSpec();
}
