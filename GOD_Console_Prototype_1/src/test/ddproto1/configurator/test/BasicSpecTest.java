/*
 * Created on Sep 5, 2005
 * 
 * file: BasicSpecTestCase.java
 */
package ddproto1.configurator.test;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ddproto1.configurator.SpecLoader;
import ddproto1.interfaces.IMessageBox;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.ILogManager;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestUtils;

public class BasicSpecTest extends TestCase{

    protected SpecLoader getDefaultSpecLoader()
        throws Exception
    {
        return ConfiguratorSetup.configureSpecLoader(
                TestUtils.getProperty(TestUtils.COMP_SPECS_DIR), 
                TestUtils.getProperty(TestUtils.MAIN_DIR) + "/" + 
                TestUtils.getProperty(TestUtils.CONFIG_DIR) + "/" +
                TestUtils.getProperty(TestUtils.SPECS_DIR));
    }

}
