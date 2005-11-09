/*
 * Created on Apr 13, 2005
 * 
 * file: IConfigurator2.java
 */
package ddproto1.configurator;

import java.net.URL;

import org.xml.sax.SAXException;


public interface IConfigurator {
    public IObjectSpec parseConfig(URL url) throws SAXException;
}
