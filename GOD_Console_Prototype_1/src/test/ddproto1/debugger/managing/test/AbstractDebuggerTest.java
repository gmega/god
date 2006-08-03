/*
 * Created on 31/07/2006
 * 
 * file: AbstractDebuggerTest.java
 */
package ddproto1.debugger.managing.test;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.debugger.managing.ILocalNodeManager;
import ddproto1.debugger.managing.LaunchHelper;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.TestConfigurationConstants;
import ddproto1.util.TestLocationConstants;
import ddproto1.util.TestUtils;
import junit.framework.TestCase;

public class AbstractDebuggerTest extends TestCase implements TestLocationConstants, TestConfigurationConstants, IConfigurationConstants{
    
    private static volatile LaunchHelper fLaunchHelper;
    
    /** Utility methods **/
    public void configureEverything()
        throws Exception
    {
        if(fLaunchHelper != null) return;
        /** Begin by loading a configuration into the plug-in. */
        String specLookup = TestUtils.getProperty(TestUtils.MAIN_DIR) + "/" + 
                            TestUtils.getProperty(TestUtils.CONFIG_DIR) + "/" +
                            TestUtils.getProperty(TestUtils.SPECS_DIR);
        
        LaunchHelper lHelper = new LaunchHelper();

        lHelper.loadDistributedSystemSpec(TestUtils.getResource(TestUtils.getProperty(TestUtils.DDTEST_CONF_FILE)),
                TestUtils.getTestPropertiesCopy(),
                ConfiguratorSetup.configureSpecLoader(specLookup, specLookup));
        
        fLaunchHelper = lHelper;
    }
    
    public LaunchHelper getLaunchHelper()
        throws Exception
    {
        return fLaunchHelper;
    }
    
    public IJavaProject getServerProject(){
        return getProtected(new ProjectGetter(TestConfigurationConstants.SERVER_PROJECT_NAME));
    }
    
    public IJavaProject getClientProject(){
        return getProtected(new ProjectGetter(TestConfigurationConstants.SERVER_PROJECT_NAME));
    }
    
    public IJavaProject getMiscProject(){
        return getProtected(new ProjectGetter(TestConfigurationConstants.MISC_PROJECT_NAME));
    }
    
    private IJavaProject getProtected(ProjectGetter getter)
    {
        do{
            try{
                return getter.getProject();
            }catch(InterruptedException ex){
                continue;
            }catch(ExecutionException ex){
                throw new RuntimeException(ex.getCause());
            }
        }while(true);
    }
    
    public IJavaProject getJavaProject(String name){
        IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        return JavaCore.create(proj);
    }
    
    public IResource getBreakpointResource(String typeName, IJavaProject project)
        throws JavaModelException
    {
        IJavaElement element = project.findElement(new Path(typeName + ".java"));
        IResource resource = element.getCorrespondingResource();
        // Don't know why the JDT folks do this.
        if(resource == null) return project.getProject();
        return resource;
    }
    public void launchApplication(String name) throws CoreException {
        ILaunchManager ilm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType dtnType = ilm
                .getLaunchConfigurationType(ID_GOD_DISTRIBUTED_APPLICATION);
        ILaunchConfiguration[] configs = ilm.getLaunchConfigurations(dtnType);

        ILaunchConfigurationWorkingCopy ilcw = null;

        for (int i = 0; i < configs.length; i++) {
            if (configs[i].getName().equals(name))
                ilcw = configs[i].getWorkingCopy();
        }

        if (ilcw == null) {
            ILaunchConfiguration conf = dtnType.newInstance(null, name);
            ilcw = conf.getWorkingCopy();
            ilcw.setAttribute(IConfigurationConstants.NAME_ATTRIB, name);
            ilcw.doSave();
        }

        ilcw.launch("debug", new NullProgressMonitor());
    }

    public void launchGA() throws CoreException {
        ILaunchManager ilm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType gaType = ilm
                .getLaunchConfigurationType(ID_GLOBAL_AGENT_APPLICATION);
        ILaunchConfiguration[] configs = ilm.getLaunchConfigurations(gaType);

        ILaunchConfigurationWorkingCopy ilcw;

        /** Checks to see if there is a config instance. */
        if (configs.length == 0) {
            /** Nope. We'll have to create one. */
            ilcw = gaType.newInstance(null, CENTRAL_AGENT_CONFIG_NAME);
        } else {
            assertTrue(configs.length == 1); // There should be only one.
            assertTrue(configs[0].getName().equals(CENTRAL_AGENT_CONFIG_NAME));
            ilcw = configs[0].getWorkingCopy();
        }

        ilcw.doSave();
        ilcw.launch("debug", new NullProgressMonitor());
    }
    
    private class ProjectGetter{
        private volatile String name;
        
        public ProjectGetter(String name) {this.name = name;}
        public IJavaProject getProject() throws InterruptedException, ExecutionException{
            return getJavaProject(TestUtils.getProperty(name));
        }
        
    }
}
