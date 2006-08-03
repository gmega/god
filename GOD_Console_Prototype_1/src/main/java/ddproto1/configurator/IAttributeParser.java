/*
 * Created on Apr 27, 2005
 * 
 * file: IAttributeParser.java
 */
package ddproto1.configurator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface IAttributeParser {
    public void parseAttribute(String qName, Attributes attributes) throws SAXException;
}
