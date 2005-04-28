/*
 * Created on Apr 25, 2005
 * 
 * file: ObjectSpecTest.java
 */
package ddproto1.test;

import java.util.LinkedList;
import java.util.List;

import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IObjectSpecType;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UninitializedAttributeException;
import junit.framework.TestCase;

public class ObjectSpecTypeImplTest extends TestCase {

    public ObjectSpecTypeImplTest() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void testAll(){
        try{
            String toc = "file:///home/giuliano/workspace/GOD Console Prototype 1/specs";
            List <String> locations = new LinkedList<String>();
            locations.add(toc);
            SpecLoader loader = new SpecLoader(locations, toc);
        
            IObjectSpecType javaNode = loader.specForName("Java", "node");
            IObjectSpec normalSpec = javaNode.makeInstance();
            IObjectSpec corbaSpec = javaNode.makeInstance();
            
            normalSpec.setAttribute("CORBA-enabled", "no");
            corbaSpec.setAttribute("CORBA-enabled", "yes");
            
            try{
                normalSpec.setAttribute("stublist", "stubby");
                fail();
            }catch(IllegalAttributeException e) { }
            
            corbaSpec.setAttribute("stublist", "stubby");
            corbaSpec.setAttribute("skeletonlist", "skeletonius");
            
            System.out.println(printHierarchy(corbaSpec, ""));
            
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    public String printHierarchy(IObjectSpec spec, String initialSpacing){
        
        StringBuffer spaces = new StringBuffer();
        
        try{
            spaces.append(initialSpacing + "[+]" + " ObjectSpec type: " + spec.getType().getInterfaceType() + "\n");
        }catch(IllegalAttributeException e){ fail();}
        
        for(String key : spec.getType().attributeKeySet()){
            String value = null;
            try{
                value = spec.getAttribute(key);
            }catch(UninitializedAttributeException ex){
                value = "<uninitialized>";
            }catch(IllegalAttributeException ex){
                fail();
            }
            
            String attribute = key + ": " + value + "\n";
            
            spaces.append(initialSpacing + " | " + attribute);
        }
        
        for(IObjectSpec child : spec.getChildren()){
            printHierarchy(child, initialSpacing + 1);
        }
        
        return spaces.toString();
    }

}
