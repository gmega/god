package ddproto1.launcher.procserver.test;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.launcher.ICommandLine;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.Lookup;
import ddproto1.util.TestUtils;
import junit.framework.TestCase;

public class LocalAgentCommandLineTest extends TestCase {
	public void testLocalAgentCommandLine()
		throws Exception
	{
		TestUtils.setPluginTest(true);

        IObjectSpec ioSpec = ConfiguratorSetup.getRoot();
		IObjectSpec _lacl = ioSpec.getChildSupporting(ICommandLine.class);
		
        IServiceLocator sLocator = (IServiceLocator)Lookup.serviceRegistry().locate(
                IConfigurationConstants.SERVICE_LOCATOR);
        
		ICommandLine lacl = (ICommandLine) sLocator.incarnate(_lacl);
		
		lacl.setAttribute(IConfigurationConstants.GUID_ATTRIBUTE, "0");
		
		String [] cline = lacl.renderCommandLine();
		
		String [] oracleLine = {
				"java", "-D"+IConfigurationConstants.LOCAL_AGENT_GID_OPT+"=0",
				"ddproto1.launcher.procserver.test.testapp.TestApplication"
		};
		
		assertTrue(cline.length == oracleLine.length);
		
		for(int i = 0; i < cline.length; i++){
			assertTrue(cline[i].equals(oracleLine[i]));
		}
	
		
	}
}
