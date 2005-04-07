/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.configurator;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.util.MessageHandler;


/**
 * @author giuliano
 *
 * This class is a mess. It's intended purpose would be to assemble a hierarchical 
 * object structure based on XML specifications and auxilary object specifications. However, 
 * this prototype version ended up tied to a simple hierarchy and is begging for a complete
 * rewrite. This happened mainly due to my poor understanding of XML/DTD (initally) which
 * led me to build something that couldn't work - this class is the actual result of (barely)
 * fixing my that initial work.
 * 
 * TODO: Plan better the configurator architecture and rewrite it.
 * 
 */
public class DefaultConfiguratorImpl extends DefaultHandler implements IConfigurator{

    private static final String module = "DefaultConfiguratorImpl -";
    
    /* These states alone are far from enough if one is trying
     * to guarantee the consistency of the XML configuration document.
     * However, when combined with the DTD in a validating parser, they
     * turn out to be everything we need.
     */
    private static final int TOP = 0;
    private static final int READING_LAUNCH_CONF = 1;
    private static final int READING_TUNNEL_CONF = 2;
    private static final int READING_SRCMAP_CONF = 3;
    private static final int FINISHED = 4;
    
    private int state;
    private NodeInfo ni;
    private Map current;
    private String tunnelPrefix;
    private String launcherPrefix;
    private String mapperPrefix;
    
    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurator#parseConfig(org.xml.sax.InputSource)
     */
    public synchronized Collection parseConfig(URL url) throws SAXException{
        
        URLConnection conn = null;
        
        try{
            
            conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputSource src = new InputSource(is);
            
            XMLReader xmlr = XMLReaderFactory.createXMLReader();
            // How bizarre is this "http" thing in our feature string??
            xmlr.setFeature("http://xml.org/sax/features/validation", true);
            current = new HashMap();
            state = TOP;
            
            xmlr.setContentHandler(this);
            xmlr.setErrorHandler(this);
            
            xmlr.parse(src);
            
        }catch(IOException e){
            convert(e);
        }
        
        return current.values();
    }
    
    // Error handling routines
    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Error while parsing " + e.getPublicId()
                        + " at line " + e.getLineNumber());
        throw e;
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Fatal error while parsing " + e.getPublicId()
                        + " at line " + e.getLineNumber());

        throw e;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Received warning while parsing " + e.getPublicId()
                        + " at line " + e.getLineNumber());
        throw e;
    }

   

    // Document parsing routines
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(module + " Begin parsing configuration file.");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(module + " Done parsing configuration file.");
    }
    
    /**
     * The good thing about XML is that it's structure induces parsers that have
     * a tree-like state graph - well, actually that depends on your grammar. Since
     * my grammar does not have circular dependencies (I don't know if that is allowed
     * in XML), we have well-defined parent states for every state in the
     * parser. That's why my endElement method is so simple.
     */
    
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        
        if(qName.equals("launch") || qName.equals("sourcemapper"))
            state = TOP;
        else if(qName.equals("tunnel"))
            state = READING_LAUNCH_CONF;
        
        
    }
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
                
        // DTD validation is all very well but it does not handle
        // a few parsing issues. It does, however, simplify the
        // number of states in our parser.
        if(qName.equals("debuggee")){
            parseDebuggee(attributes);
            insertAllAttributes("", attributes);
            
        } else if(qName.equals("tunnel")){
            // This *MUST* be true
            assert(state == READING_LAUNCH_CONF);
            // This dictates the semantics of a 'param' tag.
            state = READING_TUNNEL_CONF;
            parseTunnel(attributes);
            
        }else if(qName.equals("launch")){
            assert(state == TOP);
            state = READING_LAUNCH_CONF;
            parseLauncher(attributes);
            
        }else if(qName.equals("sourcemapper")){
            assert(state == TOP);
            state = READING_SRCMAP_CONF;
            parseMapper(attributes);
    	}
        else if(qName.equals("param")){
            parseParam(attributes);
        }
        
    }
    
    private void parseParam(Attributes attribs)
    	throws SAXException {

        String key = attribs.getValue("","key");
        String value = attribs.getValue("", "value");
        
        // The meaning of the parameter tag depends on the parser
        // state.
        int gu = -1;
        try{
            
            if(state == READING_TUNNEL_CONF){
                ni.setAttribute(tunnelPrefix + "." + key, value);
            }else if(state == READING_LAUNCH_CONF){
                ni.setAttribute(launcherPrefix + "." + key, value);
            }else if(state == READING_SRCMAP_CONF){
                ni.setAttribute(mapperPrefix + "." + key, value);
            }else{
                // Just checking.
                assert(state == TOP);
                ni.setAttribute(key, value);
            }
        }catch(IllegalAttributeException e){
            convert(e);
        }
        
    }

    // REFACTORME Guess I could merge the below parseXXX methods into one. 
    private void parseDebuggee(Attributes attribs)
        throws SAXException {
        
        MessageHandler mh = MessageHandler.getInstance();
        
        try{
            // Creates node info, inserts it into list. 
            ni = new NodeInfo();
            // This is a bit restrictive, but hey, let's take it easy.
            String curdir = System.getProperty("user.dir");
            String type = attribs.getValue("", "type");
            ni.addSpec(new URL("file://" + curdir + "/specs/" + type + ".nodespec.xml"), "");
            
            String name = attribs.getValue("", "name");
            mh.getStandardOutput().println(module + "- Adding configuration information for node " + name);
            current.put(name, ni);
            
        }catch(MalformedURLException e){
            // This is a very serious error.
            // FIXME This misuse of the assert keyword must be replaced by decent error handling code.
            assert(1 == 0);
        }
    }
    
    /**
     * @param attributes
     */
    private void parseMapper(Attributes attribs) 
    	throws SAXException
    {
        try {
            String curdir = System.getProperty("user.dir");
            String classid = attribs.getValue("", "class");

            // As usual, the prefix is the fully-qualified name.
            mapperPrefix = classid;

            int lastdot = classid.lastIndexOf('.');
            if (lastdot != -1)
                classid = classid.substring(lastdot + 1);

            ni.addSpec(new URL("file://" + curdir + "/specs/" + classid
                    + ".mapperspec.xml"), mapperPrefix + ".");

            /**
             * Class attributes always go to the detainer of the reference.
             * Another approach would be to eagerly instantiate the class and
             * pass it through to the launcher (however, we feel lazy loading
             * the tunnel is for the best, since the launcher knows better what
             * to do with it than we do).
             */
            ni.setAttribute("mapper-class", mapperPrefix);

            // Inserts the rest under the tunnel prefix.
            Set s = new HashSet();
            s.add("class");
            insertAllAttributes(mapperPrefix + ".", attribs, s);

        } catch (Exception e) {
            convert(e);
        }
    }

    
    private void parseTunnel(Attributes attribs) throws SAXException {
        try {
            String curdir = System.getProperty("user.dir");
            String classid = attribs.getValue("", "class");

            // As usual, the prefix is the fully-qualified name.
            tunnelPrefix = classid;

            int lastdot = classid.lastIndexOf('.');
            if (lastdot != -1)
                classid = classid.substring(lastdot + 1);

            tunnelPrefix = launcherPrefix + "->" + tunnelPrefix;

            ni.addSpec(new URL("file://" + curdir + "/specs/" + classid
                    + ".tunnelspec.xml"), tunnelPrefix + ".");

            /**
             * Class attributes always go to the detainer of the reference.
             * Another approach would be to eagerly instantiate the class and
             * pass it through to the launcher (however, we feel lazy loading
             * the tunnel is for the best, since the launcher knows better what
             * to do with it than we do).
             */
            insertAllAttributes(launcherPrefix + "." + "tunnel-", attribs);

        } catch (MalformedURLException e) {
            // This is a very serious error.
            // FIXME This misuse of the assert keyword must be replaced by
            // decent error handling code.
            assert (1 == 0);
        }
    }
    
    private void parseLauncher(Attributes attribs)
    	throws SAXException
    {
        try{
            String curdir = System.getProperty("user.dir");
            String classid = attribs.getValue("", "class");
            
            // The prefix is the full qualified name (to avoid name clashes)
            launcherPrefix = classid;
            int lastdot = classid.lastIndexOf('.');
            if(lastdot != -1)
                classid = classid.substring(lastdot + 1);
                        
            ni.addSpec(new URL("file://" + curdir + "/specs/" + classid + ".launcherspec.xml"), launcherPrefix + ".");
            insertAllAttributes("launcher-", attribs);

        }catch(MalformedURLException e){
            // This is a very serious error.
            // FIXME This misuse of the assert keyword must be replaced by decent error handling code.
            assert (1 == 0);
        }
    }
    
    private void insertAllAttributes(String prefix, Attributes attribs)
    	throws SAXException
    {
        insertAllAttributes(prefix, attribs, new HashSet());
    }
    
    private void insertAllAttributes(String prefix, Attributes attribs, Set exceptions)
    	throws SAXException
    {
        try{
            for(int i = 0; i < attribs.getLength(); i++){
                String key = attribs.getLocalName(i);
                if(exceptions.contains(key))
                    continue;
                ni.setAttribute(prefix + key, attribs.getValue(i));
            }
        }catch(IllegalAttributeException e){
            convert(e);
        }
    }
    
    
    private void convert(Exception e)
    	throws SAXException {

        // For now, not much.
        throw new SAXException(e);
    }
 }
