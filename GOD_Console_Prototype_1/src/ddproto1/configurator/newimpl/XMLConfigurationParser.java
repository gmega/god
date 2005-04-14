/*
 * Created on Apr 12, 2005
 * 
 * file: XMLConfigurationParser.java
 */
package ddproto1.configurator.newimpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.configurator.IConfigurator;
import ddproto1.util.MessageHandler;

public class XMLConfigurationParser extends DefaultHandler2 implements IConfigurator{
    
    private static String module = "XMLConfigurationParser -";
    
    private static final int PARSING = 1;
    private static final int IDLE = 0;
    
    private int state = IDLE;
    
    private Collection <IObjectSpec> specs;
    
    private Stack<IObjectSpec> stack = new Stack <IObjectSpec>();
    
    public Collection parseConfig(URL url) throws SAXException {
        
        synchronized(this){
            if(state == PARSING)
                throw new IllegalStateException("Parser is busy.");
            
            state = PARSING;
        }
        
        URLConnection conn = null;
        
        try {
            conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputSource src = new InputSource(is);

            XMLReader xmlr = XMLReaderFactory.createXMLReader();
            // How bizarre is this "http" thing in our feature string??
            xmlr.setFeature("http://xml.org/sax/features/validation", true);

            xmlr.setContentHandler(this);
            xmlr.setErrorHandler(this);

            xmlr.parse(src);
            
            synchronized(this){
                state = IDLE;
                return specs;
            }
            
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(module + " Done parsing configuration file.");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(module + " Begin parsing configuration file.");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        super.startElement(uri, localName, qName, attributes);
    }

}
