/*
 * Created on Apr 13, 2005
 * 
 * file: IMetaConfigurator.java
 */
package ddproto1.configurator;

import java.io.IOException;

import org.xml.sax.SAXException;


public interface ISpecLoader {
    public IObjectSpecType specForName(String concreteType, ISpecType specType)
            throws SpecNotFoundException, IOException, SAXException;
    
    public ISpecType specForName(String specType)
        throws SpecNotFoundException, IOException, SAXException;;
    
}
