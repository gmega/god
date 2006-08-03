/*
 * Created on 27/07/2006
 * 
 * file: DDTestSuite.java
 */
package ddproto1.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.test.BaseObjectSpecTypeImplTest;
import ddproto1.configurator.test.ExtendedObjectSpecTypeImplTest;
import ddproto1.configurator.test.ObjectSpecFactoryTest;
import ddproto1.configurator.test.XMLParserTest;
import ddproto1.debugger.eventhandler.test.DelegatingHandlerTest;
import ddproto1.debugger.managing.identification.test.FIFOGUIDManagerTest;
import ddproto1.debugger.managing.test.CondensedSingleNodeTest;
import ddproto1.debugger.managing.tracker.tests.DistributedThreadTest;
import ddproto1.debugger.request.test.DeferrableBreakpointRequestTest;
import ddproto1.debugger.request.test.DeferrableRequestQueueTest;
import ddproto1.debugger.server.test.SocketServerTest;
import ddproto1.launcher.procserver.test.LocalAgentCommandLineTest;
import ddproto1.launcher.procserver.test.ProcessServerManagerTest;
import ddproto1.launcher.procserver.test.SunVMCommandLineTest;
import ddproto1.launcher.test.CircularStringBufferTest;
import ddproto1.launcher.test.ExpectSSHTunnelTest;
import ddproto1.util.TestConfigurationConstants;
import ddproto1.util.TestUtils;
import ddproto1.util.test.AbstractMultiMapTest;
import ddproto1.util.test.ByteArrayTest;
import ddproto1.util.test.ConversionTest;
import ddproto1.util.test.EventTest;
import ddproto1.util.test.FormatHandlerTest;
import ddproto1.util.test.LockingHashMapTest;
import ddproto1.util.test.UtilityTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class DDTestSuite extends TestSuite {

    private static final Map <Object, Object>
        sharedObjectRegistry = Collections.synchronizedMap(new HashMap<Object, Object>());
    
    public static Test suite(){
        return new DDTestSuite();
    }
    
    public DDTestSuite(){
        /** Base tasks */
        addTest(new TestSuite(ProjectCreationDecorator.class));
        
        /** Configurator tests **/
//        addTest(new TestSuite(ObjectSpecFactoryTest.class));
//        addTest(new TestSuite(BaseObjectSpecTypeImplTest.class));
//        addTest(new TestSuite(ExtendedObjectSpecTypeImplTest.class));
//        addTest(new TestSuite(XMLParserTest.class));
        
//        /** ID manager tests */
//        addTest(new TestSuite(FIFOGUIDManagerTest.class));
//        
//        /** Utility class tests */
//        addTest(new TestSuite(AbstractMultiMapTest.class));
//        addTest(new TestSuite(ByteArrayTest.class));
//        addTest(new TestSuite(ConversionTest.class));
//        addTest(new TestSuite(EventTest.class));
//        addTest(new TestSuite(FormatHandlerTest.class));
//        addTest(new TestSuite(LockingHashMapTest.class));
//        addTest(new TestSuite(UtilityTest.class));
//        
//        /** Server/handler simulation tests */
//        addTest(new TestSuite(DelegatingHandlerTest.class));
//        addTest(new TestSuite(SocketServerTest.class));
//
//        /** Request mechanism simulation tests */
//        addTest(new TestSuite(DeferrableBreakpointRequestTest.class));
//        addTest(new TestSuite(DeferrableRequestQueueTest.class));
//                
//        /** Process server manager and launcher tests (these are 
//         * real tests) */
//        addTest(new TestSuite(SunVMCommandLineTest.class));
//        addTest(new TestSuite(LocalAgentCommandLineTest.class));
//        addTest(new TestSuite(ProcessServerManagerTest.class));
        addTest(new TestSuite(CondensedSingleNodeTest.class));
//        addTest(new TestSuite(DistributedThreadTest.class));
        
    }
    
    public static void registerSharedObject(Object key, Object object){
        sharedObjectRegistry.put(key, object);
    }
    
    public static void getSharedObject(Object key){
        if(!sharedObjectRegistry.containsKey(key))
            throw new NoSuchElementException();
    }

}
