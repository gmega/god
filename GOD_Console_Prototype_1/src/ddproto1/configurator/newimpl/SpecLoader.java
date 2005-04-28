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

import ddproto1.configurator.util.StandardAttribute;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.util.MessageHandler;

public class SpecLoader implements ISpecLoader, IAttributeConstants{

    // TODO: Move all this stuff to a configuration file (maybe the TOC?).
    private static final String ACTION_SPLIT_CHAR = ";";
    private static final String TOC_FILENAME = "TOC.xml";
    
    private static final MessageHandler mh = MessageHandler.getInstance();
    
    public static final String module = "XMLMetaConfigurator -";
    
    private Map<String, String> specMap;
    private Map<String, ObjectSpecTypeImpl> loadedSpecs;
    
    private String tocLocation;
    private List<String> specLocations;
    
    private boolean idle = true;
    
    public SpecLoader(List <String> specLocations, String tocLocation){
        // Locations (string form, URLs).
        this.tocLocation = tocLocation;
        this.specLocations = specLocations;
        
        // TOC and spec cache
        this.loadedSpecs = new HashMap<String, ObjectSpecTypeImpl>();
        
    }
    
    public synchronized ObjectSpecTypeImpl specForName(String concreteType,
            String specType) throws SpecNotFoundException, IOException,
            SAXException {

        Map TOC = getLoadTOC();
        if (!TOC.containsKey(specType))
            throw new SpecNotFoundException("Unknown specification type - "
                    + specType);

        String expectedName = makeExpected(concreteType, specType);

        if (loadedSpecs.containsKey(expectedName))
            return loadedSpecs.get(concreteType);

        InputSource is = findAndOpen(expectedName);

        /** We could pool those. We create them every time to be able to 
         * do recursive spec loading.*/
        SpecParser sParser = new SpecParser();
        sParser.setCurrentConcrete(concreteType);

        this.runParse(sParser, is);

        loadedSpecs.put(expectedName, sParser.clear());

        return loadedSpecs.get(expectedName);

    }
    
    private Map <String, String> getLoadTOC()
        throws IOException, SAXException
    {
        if(specMap != null) return specMap;
        
        URLConnection conn = null;
        URL url = new URL(tocLocation + (tocLocation.endsWith("/")?"":"/") + TOC_FILENAME );
     
        try{
            specMap = new HashMap<String, String>();
            conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputSource src = new InputSource(is);
        
            TOCParser tParser = new TOCParser();
            this.runParse(tParser, src);
        
            return specMap;
        }catch(IOException e){
            specMap = null;
            throw e;
        }catch(SAXException e){
            specMap = null;
            throw e;
        }
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
                break;
            }catch(MalformedURLException ex){ 
            }catch(IOException ex) { }
        }
        
        if(target == null)
            throw new SpecNotFoundException(cType);
        
        return new InputSource(target);
    }
    
    private class TOCParser extends DefaultHandler{

        private Locator locator;
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // TODO Auto-generated method stub
            super.endElement(uri, localName, qName);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            String name = attributes.getValue(NAME_ATTRIB);
           
            if(qName.equals(ELEMENT_ATTRIB)){
                String val = attributes.getValue(SPEC_EXT_ATTRIB);
                if(val == null)
                    throw new SAXParseException("All TOC entries must specify " + SPEC_EXT_ATTRIB, locator);
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

        private List <IAttributeParser> lexStack;
        
        private Map<String, IActionParser> actionParsers;
        
        private ObjectSpecTypeImpl current;
        
        private Locator locator;
        
        private String currentConcrete;
        
        private int level = -1;

        private SpecParser(){
            lexStack = new ArrayList<IAttributeParser>();
            actionParsers = new HashMap<String, IActionParser>();
            initDefaultLevels();
            initDefaultActionParsers();
        }
        
        private void setCurrentConcrete(String currentConcrete){
            this.currentConcrete = currentConcrete;
        }
        
        /**
         * If you modify the specs spec, here's where you should modify things as well.
         * The parser uses an array of subparsers to determine to which piece of code 
         * it should dispatch the processing of each element. 
         */
        private void initDefaultLevels(){
            lexStack.add(new IAttributeParser(){

                public void parseAttribute(String qName, Attributes attributes) throws SAXException{
                    if(!qName.equals(SPEC_ATTRIB))
                        throw new SAXParseException("Expected "+ SPEC_ATTRIB +", got " + qName, locator);
                    
                    String type = attributes.getValue(TYPE_ATTRIB);
                    current = new ObjectSpecTypeImpl(currentConcrete, type, SpecLoader.this);
                }
            });
            
            lexStack.add(new IAttributeParser(){
                public void parseAttribute(String qName, Attributes attributes) throws SAXException{
                    // Adds new attribute.
                    if(qName.equals(PROPERTY_ATTRIB)){
                        /** Standard attributes are unconstrained. Current spec doesn't provide
                         * value validation for them (though they'll certainly produce a runtime
                         * error at some point).
                         */
                        try{
                            current.addAttribute(new StandardAttribute(attributes
                                    .getValue(ID_ATTRIB), null));
                        }catch(DuplicateSymbolException ex){
                            throw new SAXParseException("Duplicate attribute detected while loading specification.", locator);
                        }
                    }
                    // Executes action strings.
                    else if(qName.equals(OPTIONAL_ATTRIB)){
                        String attribute = attributes.getValue(ID_ATTRIB);
                        String actionString = attributes.getValue(ACTION_ATTRIBUTE);
                        if(actionString == null)
                            throw new SAXParseException("Action string expected.", locator);
                        
                        String [] actions = actionString.split(ACTION_SPLIT_CHAR);
                        
                        /** Branchable attributes are constrained. We must find out and declare the
                         * value constraints before executing any action. */
                        Map <String,String> opt2act = new HashMap<String,String>();
                       
                        for(String action : actions){
                            
                            /* I really hate doing this kind of hard-coded parsing code. 
                             * I just wish I knew how to make it more flexible and less ugly. */
                            String [] actionSpec = action.split(":");
                            if(actionSpec.length != 2)
                                throw new SAXParseException("Invalid action " + action, locator);
                            
                            String option = actionSpec[0];
                            String call = actionSpec[1];
                            
                            if(opt2act.containsKey(option))
                                throw new SAXParseException("Cannot have two actions with the same branch condition ", locator);
                            
                            opt2act.put(option, call);
                        }
                        
                        /** Now we build the constrained attribute */
                        StandardAttribute stdAttr = new StandardAttribute(attribute, opt2act.keySet());
                        try{
                            current.addAttribute(stdAttr);
                        }catch(DuplicateSymbolException ex){
                            throw new SAXParseException("Duplicate attribute detected while loading specification.", locator);
                        }
                        
                        for(String option : opt2act.keySet()){
                            String call = opt2act.get(option);
                            
                            int idx = call.indexOf('(');
                            if(idx == -1)
                                throw new SAXParseException("Malformed action call " + call, locator);
                            
                            String selector = call.substring(0, idx);
                            
                            if(!actionParsers.containsKey(selector))
                                throw new SAXParseException("Unknown action " + selector, locator);
                            
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

            // Adds a supertype.
            IActionParser loadspec = new IActionParser() {
                public void action(String attribute, String condition,
                        String selector, List<String> args,
                        IObjectSpecType context) throws Exception {
                    if (args.size() != 2)
                        throw new Exception("Wrong argument number for action "
                                + selector);
                    String concrete = args.get(0);
                    String spectype = args.get(1);

                    current.bindOptionalSupertype(new BranchKey(attribute, condition), concrete, spectype);
                }
            };
            
            // Adds a child.
            IActionParser addchild = new IActionParser(){

                public void action(String attribute, String condition,
                        String selector, List<String> args,
                        IObjectSpecType context) throws Exception {
                    if(args.size() != 2)
                        throw new Exception("Wrong argument number for action " + selector);
                    
                    /** Gets the child type and multiplicity */
                    String type = args.get(0);
                    String number = args.get(1);

                    /** Conversion will throw NumberFormatException if user screws up */
                    int _number = number.equals("*")?IObjectSpecType.INFINITUM:Integer.parseInt(number);
                    
                    context.addChild(type, _number);
                }
                
            };

            // NO-OP.
            IActionParser nop = new IActionParser() {
                public void action(String attribute, String condition,
                        String selector, List<String> args,
                        IObjectSpecType context) throws Exception {
                }
            };

            actionParsers.put("loadspec", loadspec);
            actionParsers.put("addchild", addchild);
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
        
        public ObjectSpecTypeImpl clear(){
            level = -1;
            locator = null;
            ObjectSpecTypeImpl temp = current;
            current = null;
            return temp;
        }
    }
    
    public String makeExpected(String concrete, String spectype)
        throws SAXException, IOException
    {
        String translation = getLoadTOC().get(spectype);
        String expected = (concrete == null)?"":concrete + ".";
        return expected + translation + ".xml";
    }
       
    public interface IActionParser{
        public void action(String attribute, String condition, String selector, List <String> args, IObjectSpecType context) throws Exception;
    }
}
