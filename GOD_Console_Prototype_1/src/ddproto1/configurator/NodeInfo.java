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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InternalError;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 * 
 */
public class NodeInfo extends DefaultHandler implements IConfigurable {

    private static final int UNKNOWN = -1;
    private static final int LAUNCHER = 0;
    private static final int TUNNEL = 1;

    private Map valuemap;
    private Map attribute2action;

    private int attnumber;

    private String tunnelPrefix;
    private String launcherPrefix;

    private String currentPrefix = "";

    private String[] attribs = null;

    private static final String module = "NodeInfo - ";

    public void allSet() 
    	throws ConfigException
    {
        if(attnumber == 0) return;
        if(attnumber < 0){ 
            throw new InternalError("Failure - NodeInfo class has negative" +
            		" attribute number.");
        }
        
        String missing = "";
        Iterator it = valuemap.keySet().iterator();
        while(it.hasNext()){
            String key = (String)it.next();
            if(valuemap.get(key) == null)
                missing += "<"+key+">";
        }
        
        throw new ConfigException("Required attribute(s) missing: " + missing);
    }

    public NodeInfo() {
        valuemap = new HashMap();
        attribute2action = new HashMap();
        attnumber = 0;
    }

    public void setAttribute(String key, String val)
            throws IllegalAttributeException {
        if (!valuemap.containsKey(key))
                throw new IllegalAttributeException(" Illegal attribute - "
                        + key);

        if (valuemap.get(key) == null) {
            attnumber--;
            assert (attnumber >= 0);
        }

        valuemap.put(key, val);
        performActions(key, val);
    }

    public String getAttribute(String key)
            throws IllegalAttributeException {
        if (!valuemap.containsKey(key))
                throw new IllegalAttributeException(" Illegal attribute - "
                        + key);

        return ((String) valuemap.get(key));
    }

    /**
     *  Adds a new attribute. 
     * 
     * @param attrib Name of the attribute to be added
     * @param checked Adds attribute to checklist (see <b>allSet()</b> method).
     */
    public void addAttribute(String attrib)
    	throws IllegalAttributeException
    {
        if(valuemap.containsKey(attrib))
            throw new IllegalAttributeException("Cannot add - duplicate attribute - " + attrib);
        
        valuemap.put(attrib, null);
        attnumber++;
    }
    
    /**
     * This will incorporate all attributes specified in the taget XML.
     * 
     * @param url
     */
    public void addSpec(URL url, String prefix) throws SAXException {
        URLConnection conn = null;

        try {
            conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputSource src = new InputSource(is);

            XMLReader xmlr = XMLReaderFactory.createXMLReader();
            xmlr.setFeature("http://xml.org/sax/features/validation", true);

            xmlr.setContentHandler(this);
            xmlr.setErrorHandler(this);

            currentPrefix = prefix;
            
            xmlr.parse(src);

            attribs = null;

        } catch (IOException e) {
            throw new SAXException("I/O Error - ", e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Error while parsing " + e.getSystemId()
                        + " at line " + e.getLineNumber());
        throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Fatal error while parsing " + e.getSystemId()
                        + " at line " + e.getLineNumber());
        throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        String attname = attributes.getValue("", "id");
        // Piece of cake.
        if (qName.equals("attribute")) {
            valuemap.put(currentPrefix + attname, null);
        }else if(qName.equals("extension-attribute")){
            valuemap.put(currentPrefix + attname, null);
            List l = parseAction(attributes.getValue("", "action"), currentPrefix);
            attribute2action.put(currentPrefix + attname, l);
        }else if(qName.equals("spec")){
            return;
    	}else{
            // This is an "assertion way" of saying this branch should never execute.
            System.err.println(qName + " " + localName);
            assert(1 == 0);
        }
        attnumber++;
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getStandardOutput().println(
                module + " Received warning while parsing " + e.getSystemId()
                        + " at line " + e.getLineNumber());

        throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException { }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException { }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.Configurable#getAttributeKeys()
     */
    public String[] getAttributeKeys() {
        if (attribs != null) return attribs;
        
        // Lazy construction of attribute key array.
        Set keySet = valuemap.keySet();
        String[] attribs = new String[keySet.size()];
        keySet.toArray(attribs);
        
        return attribs;
    }
    
    private List parseAction(String act, String level)
    	throws SAXException
    {
        String [] actions = act.split(",");
        List actionList = new ArrayList(actions.length);
        
        for(int i = 0; i < actions.length; i++){
            int triggerSep = actions[i].indexOf(":");
            int dataSep = actions[i].indexOf("(");
            
            // Little parse check:
            if (triggerSep == -1 || triggerSep >= dataSep
                    || !actions[i].endsWith(")")) {
                throw new SAXException("Ill-formed action expression " + act);
            }
            Action action = new Action(actions[i].substring(0, triggerSep),
                    actions[i].substring(triggerSep + 1, dataSep),
                    actions[i].substring(dataSep + 1, actions[i].length() - 1),
                    level);
            
            actionList.add(action);
        }
        return actionList;
    }
    
    private void performActions(String attkey, String attval)
    	throws IllegalAttributeException
    {
        if(!attribute2action.containsKey(attkey))
            return;
        
        List l = (List)attribute2action.get(attkey);
        Iterator it = l.iterator();
        
        MessageHandler mh = MessageHandler.getInstance();

        try{
            while(it.hasNext()){
                Action c = (Action)it.next();
                if(c.getTrigger().equals(attval))
                    c.doAction();
            }
        }catch(MalformedURLException e){
            mh.getErrorOutput().print("Error while parsing action data.");
            mh.printStackTrace(e);
        }catch(SAXException e){
            mh.getErrorOutput().print("Error while loading data.");
            mh.printStackTrace(e);
        }
    }
    
    public Set getAttributesByGroup(String prefix){
        throw new UnsupportedException(); 
    }
   
    private class Action{
        
        private String trigger;
        private String actiondata;
        private String type;
        private String level;
        private boolean repeat = true;
        
        protected Action(String trigger, String type, String actiondata, String level){
            this.trigger = trigger;
            this.type = type;
            this.level = level;
            this.actiondata = actiondata;
        }
        
        protected String getTrigger(){
            return trigger;
        }
        
        protected void doAction()
        	throws IllegalAttributeException,
                SAXParseException, MalformedURLException, SAXException
        {
            if(!repeat) return;
            if(type.equals("loadspec")){
                String curdir = System.getProperty("user.dir");
                assert(curdir != null);
                URL url = new URL("file://" + curdir + "/specs/" + actiondata);
                addSpec(url, level);
                repeat = false;
            }else if(type.equals("nop")){
                return;
            }else{
                throw new IllegalAttributeException("Unrecognized action "
                        + type + " associated with the current attribute at level " 
                        + level);
            }
        }
    }
}