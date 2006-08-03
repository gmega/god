/*
 * Created on Jul 11, 2006
 * 
 * file: ProcessServerLauncherTest.java
 */
package ddproto1.launcher.test;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.GlobalAgent;
import ddproto1.launcher.IApplicationLauncher;
import ddproto1.launcher.procserver.IProcessEventListener;
import ddproto1.launcher.procserver.ProcessServerManager;
import ddproto1.launcher.procserver.test.VerifyingListener;
import ddproto1.util.Lookup;
import ddproto1.util.TestUtils;
import ddproto1.util.traits.commons.ConversionUtil;

import junit.framework.TestCase;

/** This should be ran as a plug-in test. 
 * **/
public class ProcessServerLauncherTest extends TestCase {
    
    public void testProcessServerLauncher()
        throws Exception
    {
        TestUtils.setPluginTest(true);
        
        IObjectSpec root = ConfiguratorSetup.getRoot();
        IObjectSpec launcherSpec = root.getChildSupporting(IApplicationLauncher.class);
        IServiceLocator locator = (IServiceLocator)Lookup.serviceRegistry().locate(IConfigurationConstants.SERVICE_LOCATOR);
        IApplicationLauncher launcher = (IApplicationLauncher)locator.incarnate(launcherSpec);
        
        /** We now apply our properties manually */
        for(String key : root.getAttributeKeys()){
            if(key.equals(IConfigurationConstants.LOCAL_AGENT_ADDRESS) ||
                    key.equals(IConfigurationConstants.JDWP_PORT)) continue;
            GODBasePlugin.getDefault().
                getPluginPreferences().setValue(key, root.getAttribute(key));
        }
        
        VerifyingListener listener = 
            new VerifyingListener(new InputStreamReader(
                    TestUtils.getResource(TestUtils.getProperty(TestUtils.TEST_TEXT_URL)).openStream()));
        
        ILaunch launch = getDummyLaunch();
        
        List<IProcessEventListener> listeners = new ArrayList<IProcessEventListener>();
        listeners.add(listener);
        
        IProcess eclipseProcess = launcher.launchOn(launch, listeners);
        DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
        
        eclipseProcess.getStreamsProxy().write("giuliano needs beer\n");
        
        listener.waitForDeath();
        TestCase.assertTrue(!listener.wentWrong());
        
        TestCase.assertTrue(eclipseProcess.isTerminated());
        TestCase.assertFalse(eclipseProcess.canTerminate());
        
    }
    
    protected ILaunch getDummyLaunch(){
        return (ILaunch)Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class [] {ILaunch.class},
                new InvocationHandler(){
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if(method.getName().equals("toString"))
                            return "DummyLaunch (tm)";
                        else if(method.getName().equals("hashCode"))
                            return this.hashCode();
                        else if(method.getName().equals("equals"))
                            return proxy == args[0];
                        throw new IllegalStateException("You can't call into this launch.");
                    }
                });
    }
}
