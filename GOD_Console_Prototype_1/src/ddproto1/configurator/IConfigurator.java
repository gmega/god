/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Configurator.java
 */

package ddproto1.configurator;

import java.net.URL;
import java.util.Collection;

import org.xml.sax.SAXException;

/**
 * @author giuliano
 *
 */
public interface IConfigurator {
    public static final String LIST_SEPARATOR_CHAR = ";";
    
    public Collection parseConfig(URL url) throws SAXException;
}
