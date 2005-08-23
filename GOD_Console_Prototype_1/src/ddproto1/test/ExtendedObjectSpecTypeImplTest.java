/*
 * Created on Aug 23, 2005
 * 
 * file: ExtendedObjectSpecTypeImplTest.java
 */
package ddproto1.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ddproto1.configurator.newimpl.BranchKey;
import ddproto1.configurator.newimpl.IAttribute;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IObjectSpecType;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.util.StandardAttribute;
import ddproto1.exception.AttributeAccessException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UninitializedAttributeException;
import junit.framework.TestCase;

public class ExtendedObjectSpecTypeImplTest extends TestCase {
    
    public void testExtended(){
        try{
            String toc = "file:///home/giuliano/workspace/GOD Console Prototype 1/specs";
            List <String> locations = new LinkedList<String>();
            locations.add(toc);
            SpecLoader loader = new SpecLoader(locations, toc);
        
            /** Loads the node list specification, the java node specification
             * and the shell tunnel specification.
             */
            IObjectSpecType nodeListSpec = loader.specForName(null,"node-list");
            IObjectSpec theList = nodeListSpec.makeInstance();
            IObjectSpecType javaNode = loader.specForName("Java", "node");
            IObjectSpec nodeOne = javaNode.makeInstance();
            
            IObjectSpecType sshTunnelSpec = loader.specForName("ddproto1.launcher.JVMShellLauncher", "launcher");
            
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
            IAttribute attribute = new StandardAttribute("sample-attribute", attributeValues);
            
            nodeListSpec.addAttribute(attribute);
            
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
            theList.addChild(normalSpec);
            theList.addChild(sshTunnel);
            
            /** And validate to true. */
            assertTrue(theList.validate());
            
            /** These should also be accepted. */
            theList.addChild(garbySpec);
            theList.addChild(corbaSpec);
                
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
