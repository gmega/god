/*
 * Created on Aug 23, 2005
 * 
 * file: ExtendedObjectSpecTypeImplTest.java
 */
package ddproto1.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.newimpl.BranchKey;
import ddproto1.configurator.newimpl.IAttribute;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IObjectSpecType;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.util.StandardAttribute;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import junit.framework.TestCase;

public class ExtendedObjectSpecTypeImplTest extends TestCase {
    
    public void testExtended(){
        try{
            String toc = "file://" + System.getProperty("user.dir") + "/specs";
            List <String> locations = new LinkedList<String>();
            locations.add(toc);
            SpecLoader loader = new SpecLoader(locations, toc);
        
            /** Loads the node list specification, the java node specification
             * and the shell tunnel specification.
             */
            IObjectSpecType nodeListSpec = loader.specForName(null,loader.specForName("node-list"));
            IObjectSpec theList = nodeListSpec.makeInstance();
            IObjectSpecType javaNode = loader.specForName("ddproto1.debugger.managing.VirtualMachineManager", loader.specForName("node"));
            IObjectSpec nodeOne = javaNode.makeInstance();
            
            IObjectSpecType sshTunnelSpec = loader.specForName("ddproto1.launcher.JVMShellLauncher", loader.specForName("launcher"));
            
            /** Sets the node list so that it doesn't accept any nodes. */
            nodeListSpec.removeChildConstraint(javaNode.getInterfaceType());
            
            /** Test if the constraint is actually being enforced */
            try{
                theList.addChild(nodeOne);
                fail();
            }catch(IllegalAttributeException ex){ }
            
            /** Adds a new attribute to the nodeListSpec. */
            Set <String>attributeValues = new HashSet<String>();
            attributeValues.add("yup");  // Possible attribute values.
            attributeValues.add("nope"); //
            
            /** Tries to create an illegal attribute by setting an unallowed default value. 
             * Should fail.
             */
            try{
                IAttribute attribute = new StandardAttribute("sample-attribute", "frenzy", attributeValues);
                fail();
            }catch(InvalidAttributeValueException ex){ }
            
            IAttribute attribute = new StandardAttribute("sample-attribute", "nope", attributeValues);
            
            nodeListSpec.addAttribute(attribute);
            
            /** This attribute should default to 'nope'. */
            assertTrue(theList.getAttribute("sample-attribute").equals("nope"));
            
            /** Binds two optional children with different cardinalities under one branch key. */
            
            /** First, try to bind under invalid branch keys. */
            try{
                nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample-attribute", "yes"), javaNode.getInterfaceType(), 1, 3);
                fail();
            }catch(AttributeAccessException ex){ }
            
            try{
                nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample_attribute", "yup"), javaNode.getInterfaceType(), 1, 3);
                fail();
            }catch(AttributeAccessException ex){ }
            
            /** Does the valid binding. */
            nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample-attribute", "yup"), javaNode.getInterfaceType(), 1, 3);
            nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample-attribute", "yup"), sshTunnelSpec.getInterfaceType(), 1, 1);

            /** Creates three nodes and one node ssh tunnel. */
            IObjectSpec normalSpec = javaNode.makeInstance();
            IObjectSpec corbaSpec = javaNode.makeInstance();
            IObjectSpec garbySpec = javaNode.makeInstance();
            IObjectSpec sshTunnel = sshTunnelSpec.makeInstance();
            
            /** Should fail */
            this.insertTest(theList, normalSpec);
            this.insertTest(theList, sshTunnel);
            
            theList.setAttribute("sample-attribute", "nope");
            /** Should validate to true. */
            assertTrue(theList.validate());
            /** Setting this attribute to 'yup' enables the optional children. */
            theList.setAttribute("sample-attribute", "yup");
            /** Should validate to false. */
            assertFalse(theList.validate());
            
            /** Should accept these children. */
            assertTrue(theList.getMissingChildren().get(javaNode.getInterfaceType()) == 1);
            theList.addChild(normalSpec);
            theList.addChild(sshTunnel);
            
            /** And validate to true. */
            assertTrue(theList.validate());
            
            /** These should also be accepted. */
            theList.addChild(garbySpec);
            theList.addChild(corbaSpec);
            
            /** Test context attributes (pretty simplistic test actually). */
            garbySpec.getType().addAttribute(new StandardAttribute("sample-attribute", null, attributeValues));
            
            try{
                garbySpec.getAttribute("sample-attribute");
                fail();
            }catch(UninitializedAttributeException ex){ }
            
            garbySpec.setAttribute("sample-attribute", IObjectSpec.CONTEXT_VALUE);
            
            /** The node spec context contains all of its parents. That means that
             * the linearization algorithm must eventually reach the node list and
             * return the value set there.
             */
            assertTrue(garbySpec.getAttribute("sample-attribute").equals(
                    theList.getAttribute("sample-attribute")));
            
            /** Removing an attribute should remove all the branch keys associated with it. */
            nodeListSpec.removeAttribute("sample-attribute");
            
            /** Should validate to false, since the children are leftovers. */
            assertFalse(theList.validate());
            
            /** Missing children should evaluate to negative numbers, since the 
             * allowed range is 0...0 */
            assertTrue(theList.getMissingChildren().get(javaNode.getInterfaceType()) == -3);
            assertTrue(theList.getMissingChildren().get(sshTunnelSpec.getInterfaceType()) == -1);
            
            /** Adding the attribute back and rebinding the optionals should make the instance
             * valid once more.
             */
            nodeListSpec.addAttribute(attribute);
            nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample-attribute", "yup"), javaNode.getInterfaceType(), 1, 3);
            nodeListSpec.addOptionalChildrenConstraint(new BranchKey("sample-attribute", "yup"), sshTunnelSpec.getInterfaceType(), 1, 1);

            assertTrue(theList.validate());
            
            /** Now we test default context attributes. */
            garbySpec.getType().removeAttribute("sample-attribute");
            garbySpec.getType().addAttribute(new StandardAttribute("sample-attribute", IObjectSpec.CONTEXT_VALUE, attributeValues));
            
            assertTrue(garbySpec.getAttribute("sample-attribute").equals(
                    theList.getAttribute("sample-attribute")));

                
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    private void insertTest(IObjectSpec parent, IObjectSpec child){
        try{
            parent.addChild(child);
            fail();
        }catch(AttributeAccessException ex){ }
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
