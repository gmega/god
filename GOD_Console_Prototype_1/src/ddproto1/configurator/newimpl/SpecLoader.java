/*
 * Created on Apr 13, 2005
 * 
 * file: XMLMetaConfigurator.java
 */
package ddproto1.configurator.newimpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.util.MessageHandler;

public class SpecLoader implements ISpecLoader{

    // TODO: Move all this stuff to a configuration file (maybe the TOC?).
    private static final String SPEC_ATTRIB = "spec";
    private static final String TYPE_ATTRIB = "type";
    private static final String NAME_ATTRIB = "name";
    private static final String CHILD_ATTRIB = "child";
    private static final String SPEC_EXT_ATTRIB = "extension-attribute";
    private static final String PROPERTY_ATTRIB = "attribute";
    private static final String ID_ATTRIB = "id";
    private static final String ACTION_ATTRIBUTE = "action";
    private static final String ROOT_ATTRIB = "root-element";
    private static final String ELEMENT_ATTRIB = "element";
    private static final String MULTIPLICITY_ATTRIB = "multiplicity";
    private static final String ACTION_SPLIT_CHAR = ",";
    private static final String TOC_FILENAME = "TOC.xml";
    
    private static final MessageHandler mh = MessageHandler.getInstance();
    
    public static final String module = "XMLMetaConfigurator -";
    
    private Map<String, String> specMap;
    private Map<String, IObjectSpecType> loadedSpecs;
    
    private String tocLocation;
    private List<String> specLocations;
    
    private boolean idle = true;
    private TOCParser tParser;
    private SpecParser sParser; 
    
    private String currentConcrete;
    
    public SpecLoader(List <String> specLocations, String tocLocation){
        // Locations (string form, URLs).
        this.tocLocation = tocLocation;
        this.specLocations = specLocations;
        
        // TOC and spec cache
        this.specMap = new HashMap<String, String>();
        this.loadedSpecs = new HashMap<String, IObjectSpecType>();
        
        // State pattern variant
        this.tParser = new TOCParser();
        this.sParser = new SpecParser();
    }
    
    public synchronized IObjectSpecType specForName(String concreteType, String specType)
            throws SpecNotFoundException, IOException, SAXException {
        
        
        Map TOC = getLoadTOC();
        if(!TOC.containsKey(specType)) throw new SpecNotFoundException("Unknown specification type - " + specType);

        try{
            String expectedName = makeExpected(concreteType,specType);
            
            if (loadedSpecs.containsKey(expectedName))
                return loadedSpecs.get(concreteType);

            InputSource is = findAndOpen(expectedName);
            
            currentConcrete = concreteType;
        
            this.runParse(sParser, is);
        
            loadedSpecs.put(expectedName, sParser.clear());
        
            return loadedSpecs.get(expectedName);
        }finally{
            currentConcrete = null;
        }
    }
    
    private Map <String, String> getLoadTOC()
        throws IOException, SAXException
    {
        if(specMap != null) return specMap;
        
        URLConnection conn = null;
        URL url = new URL(tocLocation + (tocLocation.endsWith("/")?"":"/") + TOC_FILENAME );
        
        InputStream is = conn.getInputStream();
        InputSource src = new InputSource(is);
        
        this.runParse(tParser, src);
        
        return specMap;
    }
    
    private void runParse(DefaultHandler dh, InputSource src)
        throws SAXException, IOException
    {
        XMLReader xmlr = XMLReaderFactory.createXMLReader();
        // How bizarre is this "http" thing in our feature string??
        xmlr.setFeature("http://xml.org/sax/features/validation", true);
        
        xmlr.setContentHandler(dh);
        xmlr.setErrorHandler(dh);
        
        xmlr.parse(src); 
    }
    
    private InputSource findAndOpen(String cType)
        throws SpecNotFoundException
    {
        InputStream target = null;        
        
        for(String urlspec : specLocations){
            try{
                URL url = new URL(urlspec + (urlspec.endsWith("/")?"":"/") + cType);
                URLConnection conn = url.openConnection();
                target = conn.getInputStream();
            }catch(MalformedURLException ex){ 
            }catch(IOException ex) { }
        }
        
        if(target == null)
            throw new SpecNotFoundException(cType);
        
        return new InputSource(target);
    }
    
    private class TOCParser extends DefaultHandler{

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

            String name = attributes.getValue(NAME_ATTRIB);
           
            if(qName.equals(ELEMENT_ATTRIB)){
                String val = attributes.getValue(SPEC_EXT_ATTRIB);
                specMap.put(name, val);
            }else if(qName.equals(ROOT_ATTRIB)){
                specMap.put(ROOT_ATTRIB, name);
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endDocument()
         */
        @Override
        public void endDocument() throws SAXException {
            String rootElement = specMap.get(ROOT_ATTRIB);
            if(rootElement == null)
                throw new SAXException("The Table Of Contents must specify a root specification.");
            
            if(!specMap.containsKey(rootElement))
                throw new SAXException("The root specification " + rootElement + " could not be found among " +
                        "the specifications declared in the TOC.");
            
        }
    }
    
    private class SpecParser extends DefaultHandler{

        private List <AttributeParser> lexStack;
        
        private Map<String, IActionParser> actionParsers;
        
        private IObjectSpecType current;
        
        private Locator locator;
        
        private int level = -1;

        private SpecParser(){
            lexStack = new ArrayList<AttributeParser>();
            actionParsers = new HashMap<String, IActionParser>();
            initDefaultLevels();
            initDefaultActionParsers();
        }
        
        /**
         * If you modify the specs spec, here's where you should modify things as well.
         * The parser uses an array of subparsers to determine to which piece of code 
         * it should dispatch the processing of each element. 
         */
        private void initDefaultLevels(){
            lexStack.add(new AttributeParser(){

                public void parseAttribute(String qName, Attributes attributes) throws SAXException{
                    if(!qName.equals(SPEC_ATTRIB))
                        throw new SAXParseException("Expected "+ SPEC_ATTRIB +", got " + qName, locator);
                    
                    String type = attributes.getValue(TYPE_ATTRIB);
                    current = new ObjectSpecTypeImpl(currentConcrete, type);
                }
            });
            
            lexStack.add(new AttributeParser(){
                public void parseAttribute(String qName, Attributes attributes) throws SAXException{
                    // Adds new attribute.
                    if(qName.equals(PROPERTY_ATTRIB)){
                        current.addAttribute(attributes.getValue(ID_ATTRIB));
                    }
                    // Executes action strings.
                    else if(qName.equals(SPEC_EXT_ATTRIB)){
                        String attribute = attributes.getValue(ID_ATTRIB);
                        String actionString = attributes.getValue(ACTION_ATTRIBUTE);
                        if(actionString == null)
                            throw new SAXParseException("Action string expected.", locator);
                        
                        String [] actions = actionString.split(ACTION_SPLIT_CHAR);
                         
                        for(String action : actions){
                            
                            /* I really hate doing this kind of code. I just wish I knew
                             * how to make it more flexible and less ugly. */
                            String [] actionSpec = action.split(":");
                            if(actionSpec.length != 2)
                                throw new SAXParseException("Invalid action " + action, locator);
                            
                            String option = actionSpec[0];
                            String call = actionSpec[1];
                            
                            int idx = call.indexOf('(');
                            if(idx == -1)
                                throw new SAXParseException("Malformed action call " + call, locator);
                            
                            String selector = call.substring(0, idx-1);
                            
                            if(!actionParsers.containsKey(selector))
                                throw new SAXParseException("Unknown action " + action, locator);
                            
                            String [] arguments = call.substring(idx+1, call.length()-1).split(",");
                            
                            List <String> actualArguments = new ArrayList<String>();
                            if(!(arguments.length == 1 && arguments[0].equals(""))) 
                                for(String argument : arguments) actualArguments.add(argument);
                            
                            try{
                                actionParsers.get(selector).action(attribute,
                                        option, selector, actualArguments,
                                        current);
                            }catch(Exception e){
                                throw new SAXException(e);
                            }
                        }
                    }
                    
                    // Add a new child
                    else if(qName.equals(CHILD_ATTRIB)){
                        String type = attributes.getValue(TYPE_ATTRIB);
                        String multiplicity = attributes.getValue(MULTIPLICITY_ATTRIB);
                        
                        int howMany;
                        if(multiplicity == null) howMany = 1;
                        else if(multiplicity.equals("infinite")) howMany = IObjectSpecType.INFINITUM;
                        else howMany = Integer.parseInt(multiplicity);
                        
                        current.addChild(type, howMany);                        
                    }else{
                        throw new SAXParseException("Unexpected symbol.", locator);
                    }
                }
            });
        }
        
        /**
         * New actions can be added here.
         */
        private void initDefaultActionParsers() {

            // Adds a child.
            IActionParser loadspec = new IActionParser() {
                public void action(String attribute, String condition,
                        String selector, List<String> args,
                        IObjectSpecType target) throws Exception {
                    if (args.size() != 2)
                        throw new Exception("Wrong argument number for action "
                                + selector);
                    String concrete = args.get(0);
                    String spectype = args.get(1);

                    current.addOptionalSet(attribute, condition, makeExpected(
                            concrete, spectype));
                }
            };

            // NO-OP.
            IActionParser nop = new IActionParser() {
                public void action(String attribute, String condition,
                        String selector, List<String> args,
                        IObjectSpecType target) throws Exception {
                }
            };

            actionParsers.put("loadspec", loadspec);
            actionParsers.put("nop", nop);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(level < 0)
                throw new SAXParseException("Internal error - assertion failure. Send a bug report as this " +
                        "is certainly a bug.", locator);
            level--;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            level++;
            if(level > lexStack.size() - 1)
                throw new SAXParseException("Unsupported nesting level detected in document structure. This probably means " +
                        "your document is wrong.", locator);
            
            lexStack.get(level).parseAttribute(qName, attributes);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startDocument()
         */
        @Override
        public void startDocument() throws SAXException { }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endDocument()
         */
        @Override
        public void endDocument() throws SAXException { }
        
        public IObjectSpecType clear(){
            level = -1;
            locator = null;
            IObjectSpecType temp = current;
            current = null;
            return temp;
        }
    }
    
    public String makeExpected(String concrete, String spectype)
        throws SAXException, IOException
    {
        String translation = getLoadTOC().get(spectype);
        return concrete + "." + translation + ".xml";
    }
    
    private interface AttributeParser {
        public void parseAttribute(String qName, Attributes attributes) throws SAXException;
    }
    
    public interface IActionParser{
        public void action(String attribute, String condition, String selector, List <String> args, IObjectSpecType target) throws Exception;
    }
}
