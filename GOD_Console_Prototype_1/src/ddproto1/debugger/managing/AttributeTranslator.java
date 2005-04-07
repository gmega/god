/*
 * Created on Aug 19, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ManagingTranslator.java
 */

package ddproto1.debugger.managing;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import ddproto1.configurator.IInfoCarrier;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UnsupportedException;

/**
 * Decorator for translating attributes.
 * 
 * @author giuliano
 *
 */
public class AttributeTranslator implements IInfoCarrier{
    private static final String module = "AttributeTranslator -";
    private IInfoCarrier source;
    private Properties p;

    public AttributeTranslator(URL properties, IInfoCarrier source)
    	throws IOException
    {
        this.source = source;
        p = new Properties();
        p.load(properties.openStream());
    }
    
    /* (non-Javadoc)
     * @see ddproto1.interfaces.IInfoCarrier#getAttribute(java.lang.String)
     */
    public String getAttribute(String key) throws IllegalAttributeException{
        
        String val = (String)p.getProperty(key); 
        
        /* Unspecified attributes go untranslated */
        if(val == null)
            return source.getAttribute(key); 
        
        /* Removes group information */
        int idx = val.indexOf('>');
        if(idx != -1)
            val = val.substring(0,idx);
                
        return source.getAttribute(val);
    }
    
    public Set getAttributesByGroup(String group)
    {
        Set groupprops = new HashSet();
        Iterator it = p.keySet().iterator();
        
        while(it.hasNext()){
            String transprop = (String)it.next();
            String prop = p.getProperty(transprop);
            if(prop.endsWith(group)) groupprops.add(transprop);
        }
        
        return groupprops;
    }
    
    public void addAttribute(String key){
        throw new UnsupportedException();
    }

    /* (non-Javadoc)
     * @see ddproto1.configurator.IInfoCarrier#getAttributeKeys()
     */
    public String[] getAttributeKeys() {
        throw new UnsupportedException();
    }
}
