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
            IObjectSpecType sshtunnel = loader.specForName("ddproto1.launcher.JVMShellLauncher", "launcher");
            IObjectSpec normalSpec = javaNode.makeInstance();
            IObjectSpec corbaSpec = javaNode.makeInstance();
            
            IObjectSpec launcherSpec = sshtunnel.makeInstance();
            
            normalSpec.setAttribute("CORBA-enabled", "no");
            normalSpec.setAttribute("launcher", "yes");
            corbaSpec.setAttribute("CORBA-enabled", "yes");
            corbaSpec.setAttribute("launcher", "no");
            
            try{
                normalSpec.setAttribute("stublist", "stubby");
                fail();
            }catch(IllegalAttributeException e) { }

            normalSpec.addChild(launcherSpec);
            
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
