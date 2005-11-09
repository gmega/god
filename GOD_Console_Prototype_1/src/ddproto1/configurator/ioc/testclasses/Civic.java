/*
 * Created on May 18, 2005
 * 
 * file: Civic.java
 */
package ddproto1.configurator.ioc.testclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.StandardServiceLocator;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.commons.IllegalAttributeException;

public class Civic implements IConfigurable{
    
    private List <Tire> tires;
    private Engine engine;
    private Map<String, String> atts = new HashMap<String,String>();

    public void setAttribute(String key, String value) throws IllegalAttributeException {
        atts.put(key, value);
    }

    public String getAttribute(String key) throws IllegalAttributeException {
        String att = atts.get(key);
        if(att == null) throw new IllegalAttributeException("Attribute not recognized");
        return att;
    }
    
    public void start() throws IllegalAttributeException, IncarnationException{
        IServiceLocator locator = StandardServiceLocator.getInstance();
        List<IObjectSpec> children = locator.getMetaobject(this).getChildren();
        
        List <Tire> tires = new ArrayList<Tire>(4);
        Engine _engine = null;
        
        for(IObjectSpec child : children){
            if(child.getType().getInterfaceType().equals("tire"))
                tires.add((Tire)locator.incarnate(child));
            else if(child.getType().getInterfaceType().equals("engine"))
                _engine = (Engine)locator.incarnate(child);
        }
        
        this.tires = tires;
        this.tires.size();
        engine = _engine;
        engine.hashCode();
        
        System.out.println(getAttribute("name") + " started. My color is " + getAttribute("color"));
    }

    public Set<String> getAttributeKeys() {
        return null;
    }

    public boolean isWritable() {
        // TODO Auto-generated method stub
        return false;
    }
}
