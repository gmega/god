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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.configurator.IConfigurator;
import ddproto1.util.MessageHandler;

public class XMLConfigurationParser extends DefaultHandler2 implements
        IConfigurator, IAttributeConstants {

    private static String module = "XMLConfigurationParser -";
    private static final int PARSING = 1;
    private static final int IDLE = 0;
    private int state = IDLE;

    private IObjectSpec rootSpec;
    private Stack<IObjectSpec> stack = new Stack<IObjectSpec>();
    private Map<String, IAttributeParser> parsers = new HashMap<String, IAttributeParser>();
    private IAttributeParser specInstanceParser;
    private SpecLoader loader;
    private Locator locator;

    private XMLConfigurationParser() {
    }

    public XMLConfigurationParser(SpecLoader loader) {
        this.loader = loader;
        initAllParsers();
    }

    private void initAllParsers() {

        IAttributeParser propertyParser = new IAttributeParser() {
            public void parseAttribute(String qName, Attributes attributes)
                    throws SAXException {
                if (stack.size() == 0)
                    throw new SAXParseException(
                            "Attributes must be enclosed into something.",
                            locator);

                String key = attributes.getValue(PARAM_KEY_ATTRIB);
                String val = attributes.getValue(PARAM_VAL_ATTRIB);

                IObjectSpec cSpec = stack.peek();
                try {
                    cSpec.setAttribute(key, val);
                } catch (Exception ex) {
                    throw new SAXParseException(ex.getMessage(), locator, ex);
                }
            }
        };

        parsers.put(PROPERTY_ATTRIB, propertyParser);

        specInstanceParser = new IAttributeParser() {
            public void parseAttribute(String qName, Attributes attributes)
                    throws SAXException {
                String specType = qName;
                String concreteType = attributes.getValue(TYPE_ATTRIB);

                try {
                    /** Loads the specification for this item. */
                    IObjectSpecType spec = loader.specForName(concreteType,
                            specType);

                    IObjectSpec newSpec = spec.makeInstance();

                    if (!stack.isEmpty()) {
                        IObjectSpec scopeObject = stack.peek();
                        scopeObject.addChild(newSpec);
                    }

                    /** Pushes a new instance onto the stack */
                    stack.push(newSpec);

                } catch (Exception e) {
                    throw new SAXParseException(
                            "Error while loading specification type "
                                    + specType, locator, e);
                }
            }
        };

    }

    public IObjectSpec parseConfig(URL url) throws SAXException {

        synchronized (this) {
            if (state == PARSING)
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

            synchronized (this) {
                state = IDLE;
                return rootSpec;
            }

        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Done parsing configuration file.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Begin parsing configuration file.");
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        IAttributeParser assigned = parsers.get(qName);
        if(assigned == null)
            specInstanceParser.parseAttribute(qName, attributes);
        else
            assigned.parseAttribute(qName, attributes);
    }
}
