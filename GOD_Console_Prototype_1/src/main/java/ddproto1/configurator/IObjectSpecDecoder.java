/*
 * Created on Nov 26, 2005
 * 
 * file: IObjectSpecDecoder.java
 */
package ddproto1.configurator;

import java.io.IOException;

import org.xml.sax.SAXException;

import ddproto1.exception.FormatException;

public interface IObjectSpecDecoder {
    public IObjectSpec restoreFromString(String encodedSpec) throws FormatException, SpecNotFoundException, IOException, SAXException,
    InstantiationException;
}
