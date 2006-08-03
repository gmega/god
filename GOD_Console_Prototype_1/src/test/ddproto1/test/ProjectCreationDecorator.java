/*
 * Created on 27/07/2006
 * 
 * file: ProjectCreationDecorator.java
 */
package ddproto1.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.testplugin.JavaProjectHelper;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;

import ddproto1.util.DelayedResult;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestConfigurationConstants;
import ddproto1.util.TestUtils;

import junit.framework.TestCase;

public class ProjectCreationDecorator extends TestCase implements TestConfigurationConstants {
    
    public static final Logger logger = MessageHandler.getInstance().getLogger(ProjectCreationDecorator.class);
    
    public static IJavaProject fServerProject;
    public static IJavaProject fClientProject;
    public static IJavaProject fMiscProject;
    
    public void testSetPreferences(){
        Hashtable<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
        JavaCore.setOptions(options);
        
        IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
        // Don't prompt for perspective switching
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND, MessageDialogWithToggle.ALWAYS);
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE, MessageDialogWithToggle.ALWAYS);
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE, MessageDialogWithToggle.NEVER);
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_WAIT_FOR_BUILD, MessageDialogWithToggle.ALWAYS);
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS);
        debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH, MessageDialogWithToggle.NEVER);
    }
    
    public void testCreateProjects()
        throws Exception
    {
        /** Some of this code has been sucked right out of the
         * JDT test suite. Great source.
         * 
         * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.jdt.debug.tests/
         */
        // delete any pre-existing project
        ensureDeleted(SERVER_PROJECT_NAME);
        ensureDeleted(CLIENT_PROJECT_NAME);
        ensureDeleted(MISC_PROJECT_NAME);
        
        // create project and import source
        fServerProject = JavaProjectHelper.createJavaProject(TestUtils.getProperty(SERVER_PROJECT_NAME), "bin");
        fClientProject = JavaProjectHelper.createJavaProject(TestUtils.getProperty(CLIENT_PROJECT_NAME), "bin");
        fMiscProject = JavaProjectHelper.createJavaProject(TestUtils.getProperty(MISC_PROJECT_NAME));
        
        configureProject(fServerProject, TestUtils.getProperty(TEST_PROJECTS_HOME) + "/" + 
                                TestUtils.getProperty(SERVER_PROJECT_NAME));
        configureProject(fClientProject,  TestUtils.getProperty(TEST_PROJECTS_HOME) + "/" + 
                                TestUtils.getProperty(CLIENT_PROJECT_NAME));
        configureProject(fMiscProject,  TestUtils.getProperty(TEST_PROJECTS_HOME) + "/" + 
                TestUtils.getProperty(MISC_PROJECT_NAME));
    }
    
    private void configureProject(IJavaProject proj, String rootDir)
        throws Exception
    {
        String rootSrcDir = rootDir + "/src";
        String rootConfDir = rootDir + "/config";
        
        IPackageFragmentRoot rt = 
            JavaProjectHelper.addSourceContainer(proj, "src");
        
        File root = new File(rootSrcDir);
        assertTrue(root.exists());
        
        JavaProjectHelper.importFilesFromDirectory(root, rt.getPath(), null);
        
        /** Adds rt.jar **/
        IVMInstall vmi = JavaRuntime.getDefaultVMInstall();
        assertNotNull(vmi);
        JavaProjectHelper.addContainerEntry(proj, new Path(JavaRuntime.JRE_CONTAINER));
        
        File jacorbHome = new File(TestUtils.getProperty(JACORB_HOME));
        assertTrue(jacorbHome.exists());
        
        List<IClasspathEntry> cpathEntries = new ArrayList<IClasspathEntry>();
        
        /** Now adds the JacORB libs **/
        for(String libVar : TestUtils.getProperty(JACORB_FULL_LIBS).split(":")){
            IClasspathEntry varEntry = JavaCore.newVariableEntry(
                    new Path("JACORB_HOME/" + TestUtils.getProperty(libVar)),
                    null, 
                    null, 
                    false); 
            cpathEntries.add(varEntry);
        }
        
        // Adds the config to the classpath as well.
        IFolder linkedConf = proj.getProject().getFolder("config");
        linkedConf.createLink(new Path(rootConfDir), IResource.NONE, null);
        
        cpathEntries.add(JavaCore.newLibraryEntry(linkedConf.getFullPath(), null, null));
        JavaCore.setClasspathVariable("JACORB_HOME", new Path(jacorbHome
                .getAbsolutePath()), null);
        
        IClasspathEntry [] oldEntries = proj.getRawClasspath();
        IClasspathEntry [] allEntries = new IClasspathEntry[cpathEntries.size() + oldEntries.length];
        
        cpathEntries.toArray(allEntries);
        System.arraycopy(oldEntries, 0, allEntries, cpathEntries.size(), oldEntries.length);
        
        proj.setRawClasspath(allEntries, null);
    }
    
    public void testBuildProjects() throws Exception{
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, 
                new NullProgressMonitor());
        waitForBuild();
    }
    
    public void testBuildSuccessful()
        throws Exception
    {
        testSuccessfulBuildFor(fServerProject);
        testSuccessfulBuildFor(fClientProject);
    }
    
    private void testSuccessfulBuildFor(IJavaProject jProject) throws Exception{
        /** What makes a successful build? No build errors, of course. */
        IProject proj = jProject.getProject();
        IMarker [] markers = proj.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        boolean fail = false;
        for(IMarker marker : markers){
            Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
            if(severity != null && severity.intValue() >= IMarker.SEVERITY_ERROR){
                fail = true;
                logger.error(marker.getAttribute(IMarker.MESSAGE));
            }
        }
        assertFalse("Build errors found.", fail);

    }
    
    public static void waitForBuild() {
        boolean wasInterrupted = false;
        do {
            try {
                Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
                Platform.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
                wasInterrupted = false;
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        } while (wasInterrupted);
    }
    
    
    private void ensureDeleted(String projName)
        throws CoreException
    {
        IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
        if(proj.exists())
            proj.delete(true, true, null);
    }

}
