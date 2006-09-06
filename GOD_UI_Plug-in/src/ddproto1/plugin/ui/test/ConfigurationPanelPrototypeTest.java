/*
 * Created on Sep 3, 2005
 * 
 * file: ConfigurationPanelPrototypeTest.java
 */
package ddproto1.plugin.ui.test;

import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import ddproto1.configurator.IObjectSpec;
import ddproto1.plugin.ui.launching.ConfigurationPanel;

/**
 * This isn't really a test, but a support class to test the 
 * configuration view.  
 * 
 * @author giuliano
 *
 */
public class ConfigurationPanelPrototypeTest extends TestCase{

    private IObjectSpec root;

    public void testConfigPanel()
        throws Exception
    {
        ConfigPanel cppt = new ConfigPanel();
        cppt.setBlockOnOpen(true);
        cppt.open();
        Display.getCurrent().dispose();
    }
    
    private class ConfigPanel extends ApplicationWindow {

        public ConfigPanel(){
            super(null);
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected Control createContents(Composite parent) {
            this.getShell().setText("Configuration Window");
            ConfigurationPanel proto = new ConfigurationPanel(parent, SWT.NONE);
            System.err.println(System.getProperty("java.class.path"));
            try {
                proto.setObjectSpecRoot(root.getChildren().get(0).getChildren()
                        .get(0));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            parent.setSize(700, 400);
            return parent;
        }
    }
   
}
