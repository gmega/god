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
            
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

}
