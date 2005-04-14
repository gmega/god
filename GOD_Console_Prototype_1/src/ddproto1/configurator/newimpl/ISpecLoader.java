/*
 * Created on Apr 13, 2005
 * 
 * file: IMetaConfigurator.java
 */
package ddproto1.configurator.newimpl;

import java.io.IOException;

import org.xml.sax.SAXException;


public interface ISpecLoader {
    public IObjectSpecType specForName(String concreteType, String specType)
            throws SpecNotFoundException, IOException, SAXException;
}
