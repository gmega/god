/*
 * Created on Sep 3, 2005
 * 
 * file: ConfigurationPanelPrototypeTest.java
 */
package ddproto1.plugin.ui.test;

import java.io.IOException;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import ddproto1.configurator.IObjectSpec;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.launching.ConfigurationPanel;
import ddproto1.test.XMLParserTest;


public class ConfigurationPanelPrototypeTest extends ApplicationWindow{

    private IObjectSpec root;

    public ConfigurationPanelPrototypeTest() {
        super(null);
        XMLParserTest parserTest = new XMLParserTest();
        parserTest.setUp();
        parserTest.testParseConfig();
        this.root = parserTest.getRoot();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        this.getShell().setText("Configuration Window");
        ConfigurationPanel proto = new ConfigurationPanel(parent, SWT.NONE);
        System.err.println(System.getProperty("java.class.path"));
        try{
            proto.setObjectSpecRoot(root.getChildren().get(0).getChildren().get(0));
        }catch(IOException ex){ ex.printStackTrace(); } 
        parent.setSize(700, 400);
        return parent;
    }
    
    public static void main(String [] args){
        ConfigurationPanelPrototypeTest cppt = new ConfigurationPanelPrototypeTest();
        cppt.setBlockOnOpen(true);
        cppt.open();
        Display.getCurrent().dispose();
    }
   
}
