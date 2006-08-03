/*
 * Created on Apr 25, 2005
 * 
 * file: ObjectSpecTest.java
 */
package ddproto1.configurator.test;

import java.util.LinkedList;
import java.util.List;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.SpecLoader;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.JVMShellLauncher;
import ddproto1.launcher.test.ConfiguratorSetup;
import ddproto1.util.TestUtils;
import junit.framework.TestCase;

public class BaseObjectSpecTypeImplTest extends BasicSpecTest {

    public BaseObjectSpecTypeImplTest() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void testBase(){
        try{
            TestUtils.setPluginTest(true);
          
            SpecLoader loader = getDefaultSpecLoader();
        
            /** Loads the node list specification, the java node specification
             * and the shell tunnel specification.
             */
            IObjectSpecType nodeListSpec = loader.specForName(null,loader.specForName("node-list"));
            IObjectSpecType javaNode = loader.specForName(VirtualMachineManager.class.getName(), loader.specForName("node"));
            IObjectSpecType sshtunnel = loader.specForName(JVMShellLauncher.class.getName(), loader.specForName("launcher"));
            
            /** Creates three nodes and one node list. */
            IObjectSpec normalSpec = javaNode.makeInstance();
            IObjectSpec corbaSpec = javaNode.makeInstance();
            IObjectSpec garbySpec = javaNode.makeInstance();
            IObjectSpec nodeList = nodeListSpec.makeInstance();
            
            /** Modify the node list cardinality constraint for children of type
             * 'node' so that it's valid only if has more than one children and less
             * than three.
             */
            nodeList.getType().addChildConstraint(javaNode.getInterfaceType(), 1, 2);
            /** Should return false */
            assertFalse(nodeList.validate());
            nodeList.addChild(normalSpec);
            /** Should return true */
            assertTrue(nodeList.validate());
            nodeList.addChild(corbaSpec);
            /** Should return true again*/
            assertTrue(nodeList.validate());
            /** Should not allow */
            try{
                nodeList.addChild(garbySpec);
                fail();
            }catch(IllegalAttributeException ex){ }
            
            /** Modify cardinality constraint to allow one more child of type 'node'*/
            nodeList.getType().addChildConstraint(javaNode.getInterfaceType(), 1, 3);
            /** Now it should allow us to add another child */
            nodeList.addChild(garbySpec);
            /** And should pass validation */
            assertTrue(nodeList.validate());
            /** Modify cardinality constraint once more */
            nodeList.getType().addChildConstraint(nodeListSpec.getInterfaceType(), 1, 2);
            /** Should invalidate our nodelist instance */
            assertFalse(nodeList.validate());
            
            /** Now does some testing with optional attributes */
            IObjectSpec launcherSpec = sshtunnel.makeInstance();
            
            /** Sets an optional attribute to two distinct values */
            normalSpec.setAttribute("CORBA-enabled", "no");
            normalSpec.setAttribute("launcher", "yes");
            corbaSpec.setAttribute("CORBA-enabled", "yes");
            corbaSpec.setAttribute("launcher", "no");
            
            try{
                normalSpec.setAttribute("stublist", "stubby");
                fail();
            }catch(IllegalAttributeException e) { }

            normalSpec.addChild(launcherSpec);

            /** Testing the constraints never hurts. */
            try{
                corbaSpec.addChild(launcherSpec.getType().makeInstance());
                fail();
            }catch(IllegalAttributeException e){ }
            
            try{
                corbaSpec.addChild(launcherSpec);
                fail();
            }catch(IllegalAttributeException e){ }
            
            corbaSpec.setAttribute("stublist", "stubby");
            corbaSpec.setAttribute("skeletonlist", "skeletonius");
            
            System.out.println(printHierarchy(corbaSpec, "", ""));
            System.out.println(printHierarchy(normalSpec, "", ""));
            
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    public String printHierarchy(IObjectSpec spec, String initialSpacing, String fLine){
        
        StringBuffer spaces = new StringBuffer();
        
        try{
            if(fLine != null) { spaces.append(fLine); fLine = null; }
            else spaces.append(initialSpacing);
            spaces.append("[+]" + " ObjectSpec type: " + spec.getType().getInterfaceType() + "\n");
        }catch(IllegalAttributeException e){ fail();}
        
        for(String key : spec.getType().attributeKeySet()){
            String value = null;
            try{
                value = spec.getAttribute(key);
            }catch(UninitializedAttributeException ex){
                value = "<uninitialized>";
            }catch(IllegalAttributeException ex){
                value = "<unsupported>";
            }
            
            String attribute = key + ": " + value + "\n";
            
            spaces.append(initialSpacing + " | " + attribute);
        }
        
        for(IObjectSpec child : spec.getChildren()){
            spaces.append(printHierarchy(child, initialSpacing + " |  ", "[+]-"));
        }
        
        spaces.append(initialSpacing + " |> \n");
        
        return spaces.toString();
    }

}
