/*
 * Created on Apr 12, 2005
 * 
 * file: XMLConfigurationParser.java
 */
package ddproto1.configurator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.MessageHandler;

public class XMLConfigurationParser extends DefaultHandler2 implements
        IConfigurator, IConfigurationConstants {

    private static String module = "XMLConfigurationParser -";
    private static final int PARSING = 1;
    private static final int IDLE = 0;
    private int state = IDLE;
    
    private static final Logger logger = 
    		MessageHandler.getInstance().getLogger(XMLConfigurationParser.class);

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

                String key = getCheck(PARAM_KEY_ATTRIB, attributes);
                String val = getCheck(PARAM_VAL_ATTRIB, attributes);

                IObjectSpec cSpec = stack.peek();
                try {
                    cSpec.setAttribute(key, val);
                } catch (Exception ex) {
                    throw new SAXParseException(ex.getMessage(), locator, ex);
                }
            }
        };

        parsers.put(PARAM_ATTRIB, propertyParser);

        specInstanceParser = new IAttributeParser() {
            public void parseAttribute(String qName, Attributes attributes)
                    throws SAXException {
                String specType = qName;
                String concreteType = attributes.getValue(TYPE_ATTRIB);
                int exclude = attributes.getIndex(TYPE_ATTRIB);

                try {
                    /** Loads the specification for this item. */
                    IObjectSpecType spec = loader.specForName(concreteType,
                            loader.specForName(specType));

                    IObjectSpec newSpec = spec.makeInstance();

                    /** The object being pushed is son of the object just below. */ 
                    if (!stack.isEmpty()) {
                        IObjectSpec scopeObject = stack.peek();
                        scopeObject.addChild(newSpec);
                    }else{
                        if(rootSpec != null)
                            throw new SAXParseException("Assertion failed - cannot have two roots!", locator);
                        rootSpec = newSpec;
                    }
                    
                    /** Inserts the attributes packed into the declaration tag. */
                    for(int i = 0; i < attributes.getLength(); i++){
                        if(i == exclude) continue;
                        String lName = attributes.getLocalName(i);
                        newSpec.setAttribute(lName, attributes.getValue(i));
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
    
    public IObjectSpec parseConfig(URL url)
    		throws SAXException
    {
    		return this.parseConfig(url, null);
    }

    public IObjectSpec parseConfig(URL url, Properties props) throws SAXException {

        synchronized (this) {
            if (state == PARSING)
                throw new IllegalStateException("Parser is busy.");

            state = PARSING;
            if(rootSpec != null) throw new SAXException("Invariant check failure. Parser is damaged (please report this bug).");
        }

        URLConnection conn = null;

        try {
            conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputSource src = null;
            
            if(props != null){
            		String processed = ConfiguratorUtils.tokenReplace(is, props);
            		src = new InputSource(new StringReader(processed));
            }else{
                src = new InputSource(is);
            }


            XMLReader xmlr = XMLReaderFactory.createXMLReader();
            // How bizarre is this "http" thing in our feature string??
            xmlr.setFeature("http://xml.org/sax/features/validation", true);

            xmlr.setContentHandler(this);
            xmlr.setErrorHandler(this);

            xmlr.parse(src);

            synchronized (this) {
                state = IDLE;
                IObjectSpec tmp = rootSpec;
                rootSpec = null;
                return tmp;
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
        
        
        /** We only pop stuff if the element that's ending is found to be a specification. */
        if(qName.equals(PARAM_ATTRIB)) return;
        
        /** It's a spec. Pop it. */
        if(stack.size() == 0){
            throw new SAXParseException("Stack size mismatch - malformed XML?", locator);
        }

        /** Asserts that the type being popped equals to the ending type. */        
        IObjectSpec ios = stack.pop();
        
        String intf = null;
        try{  
            intf = ios.getType().getInterfaceType();
        } catch(IllegalAttributeException e) { }

        if (intf == null || !intf.equals(qName))
            throw new SAXException(
                    "Assertion failed - element being closed differs from expected element. This is most likely a bug.");
        
        /** Another verification - is the spec fully initialized? */
        if(!ios.validate()){
            StringBuffer mesg = new StringBuffer();
            List<String> unassignedKeys = ios.getUnassignedAttributeKeys();
            Map<String, Integer> unassignedChildren = ios.getMissingChildren();
            mesg.append("Missing ");
            if(!unassignedKeys.isEmpty()){
                mesg.append("attributes ");
                mesg.append(unassignedKeys);
                mesg.append(" ");
            }
            
            if(!unassignedChildren.isEmpty()){
                mesg.append("children ");
                mesg.append(unassignedChildren);
                mesg.append(" ");
            }
            
            mesg.append("from specification (" + intf + ")");
            throw new SAXException(mesg.toString());
        }
        
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
    
    private String getCheck(String key, Attributes attribs) throws SAXException{
        String val = attribs.getValue(key);
        if (val == null) throw new SAXException("Parameter " + key + " must be preceded by a valid value attribute.");
        return val;
    }
}
