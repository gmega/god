/*
 * Created on Apr 13, 2005
 * 
 * file: IConfigurator2.java
 */
package ddproto1.configurator;

import java.net.URL;

import org.xml.sax.SAXException;

import ddproto1.configurator.newimpl.IObjectSpec;

public interface IConfigurator {
    public static final String LIST_SEPARATOR_CHAR = ";";
    
    public IObjectSpec parseConfig(URL url) throws SAXException;
}
