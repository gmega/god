/*
 * Created on Oct 17, 2005
 * 
 * file: ObjectSpecTypeMementoFactory.java
 */
package ddproto1.configurator;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.FormatException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.util.IStringEncoder;
import ddproto1.util.collection.UnorderedMultiMap;

/**
 * This auxiliary object transforms IObjectSpecs into Strings
 * and vice-versa.  
 * 
 * @author giuliano
 *
 */
public class ObjectSpecStringfier {
    
    private static final String NODE_SEPARATOR_CHAR = "-";
    private static final String ITEM_SEPARATOR_CHAR = ";";
    private static final String TYPE_TUPLE_START = "(";
    private static final String TYPE_TUPLE_END = ")";
    private static final String ATTRIBUTE_LIST_START = "<";
    private static final String ATTRIBUTE_LIST_END = ">";
    private static final String ATTRIBUTE_SPLITTER = ":";
    
    private static String [] disallowedChars = new String[] {
            NODE_SEPARATOR_CHAR,
            ITEM_SEPARATOR_CHAR,
            TYPE_TUPLE_START,
            TYPE_TUPLE_END,
            ATTRIBUTE_LIST_START,
            ATTRIBUTE_LIST_END,
            ATTRIBUTE_SPLITTER
    };
    
    private IStringEncoder encoder;
    private ISpecLoader loader;
    
    public ObjectSpecStringfier(ISpecLoader loader, IStringEncoder encoder){
        this.loader = loader;
        this.encoder = encoder;
        encoder.setDisallowedCharacters(disallowedChars);
    }
    
    public IObjectSpec restoreFromString(String spec) 
        throws FormatException, SpecNotFoundException, IOException, SAXException,
        InstantiationException
    {
        StringReader sReader = new StringReader(spec);
        StringObjectSpecLexer lexer = new StringObjectSpecLexer(sReader);
        SpecToken token = checkGetToken(SpecToken.NUMBER, lexer);
        Map<Integer, IObjectSpec> id2Spec = new HashMap<Integer, IObjectSpec>();
        
        int nElements = Integer.parseInt(token.text);
        for(int i = 0; i < nElements; i++){
            
            token = checkGetToken(SpecToken.NUMBER, lexer);
            int elementId = Integer.parseInt(token.text);
            token = checkGetToken(SpecToken.TYPE_TUPLE, lexer);
            String[] theTuple = token.text.substring(1, token.text.length()-1)
                    .split("\\Q" + ITEM_SEPARATOR_CHAR + "\\E");
            
            String abstractType = encoder.decode(theTuple[0]);
            String concreteType = (theTuple.length == 1)?null:encoder.decode(theTuple[1]);
            
            /** Attempts to load the actual spec. */
            ISpecType sType = loader.specForName(abstractType);
            IObjectSpecType cType = loader.specForName(concreteType, sType);
            
            /** Reads all attributes. */
            Map<String, String> attributes = new HashMap<String,String>();
            for(token = lexer.yylex(); token.type == SpecToken.ATTRIBUTE_TUPLE; token = lexer.yylex()){
                String [] attributePair = token.text.split(ATTRIBUTE_SPLITTER);
                String attributeValue = (attributePair.length == 1)?"":attributePair[1];
                attributeValue = attributeValue.equals(IObjectSpec.CONTEXT_VALUE)?
                        IObjectSpec.CONTEXT_VALUE:encoder.decode(attributeValue);
                    
                attributes.put(encoder.decode(attributePair[0]), attributeValue);
            }
            
            /** Tries to apply all attributes. */
            try{
                IObjectSpec instance = cType.makeInstance();
                applyAttributes(instance, attributes);
                id2Spec.put(elementId, instance);
            }catch(AttributeAccessException ex){
                throw new FormatException("This string no longer represents a valid specification tree. " +
                        "Maybe the specs have changed?");
            }
            
        }
        
        /** Okay, we're done with the elements, now we read the child associations. */
        for(int i = 0; i < nElements; i++){
            token = checkGetToken(SpecToken.CHILD_LIST, lexer);
            if(token == null) break;
            String [] childList = token.text.split(NODE_SEPARATOR_CHAR);
            int elementId = Integer.parseInt(childList[0]);
            IObjectSpec ospec = id2Spec.get(elementId);
            if(ospec == null) throw new FormatException();
            
            for(int k = 1; k < childList.length; k++){
                IObjectSpec childSpec = id2Spec.get(Integer.parseInt(childList[k]));
                if(childSpec == null) throw new FormatException();
                try{
                    ospec.addChild(childSpec);
                }catch(IllegalAttributeException ex){
                    throw new FormatException("This string no longer represents a valid specification tree. " +
                        "Maybe the specs have changed?");
                }
            }
        }
        
        return id2Spec.get(0);
    }
    
    private void applyAttributes(IObjectSpec spec, Map<String, String> attributes)
        throws InstantiationException, AttributeAccessException
    {
        Set<String> applied = new HashSet<String>();
        while(true){
            Set<String> toApply = spec.getAttributeKeys();
            toApply.removeAll(applied);
            if(toApply.size() == 0) break;
            for(String key : toApply){
                if(attributes.containsKey(key))
                    spec.setAttribute(key, attributes.get(key));
                applied.add(key);
            }
        }
    }
    
    private SpecToken checkGetToken(Object type, StringObjectSpecLexer lexer)
        throws FormatException, IOException
    {
        SpecToken token = lexer.yylex();
        if(token == null) return null;
        if(token.type != type) 
            throw new FormatException();
        return token;
    }
    
    public String makeFromObjectSpec(IObjectSpec spec)
    {
        UnorderedMultiMap<Integer, Integer> childMap = new UnorderedMultiMap<Integer, Integer>(
                HashSet.class);  
        
        Map<IObjectSpec, Integer> spec2Id = new HashMap<IObjectSpec, Integer>();
        
        /** First we build the child map. */
        Iterator it = new ChildrenDFSIterator(spec);
        int id = 0;
        spec2Id.put(spec, id);
        IObjectSpec next = spec;
                
        while(true){
            int pId = spec2Id.get(next);
            for(IObjectSpec child : next.getChildren()){
                int cId;
                if(spec2Id.containsKey(child)) cId = spec2Id.get(child);
                else{
                    cId = ++id;
                    spec2Id.put(child, cId);
                }
                childMap.add(pId, cId);
            }
            if(!it.hasNext()) break;
            next = (IObjectSpec)it.next();
        }

        StringBuffer stringForm = new StringBuffer();
        /** Now we build the actual string representation. First the definitions.*/
        stringForm.append(spec2Id.keySet().size());
        stringForm.append(ITEM_SEPARATOR_CHAR);
        for(IObjectSpec cSpec : spec2Id.keySet()){
            stringForm.append(spec2Id.get(cSpec));
            String concrete = "";
            String iFace = "";
            try{
                concrete = cSpec.getType().getConcreteType();
            }catch(IllegalAttributeException ex){ }
            
            try{
                iFace = cSpec.getType().getInterfaceType();
            }catch(IllegalAttributeException ex){ 
                throw new InternalError("Object specifications must specify an interface type.");
            }
            
            stringForm.append(ITEM_SEPARATOR_CHAR);
            stringForm.append(TYPE_TUPLE_START);
            stringForm.append(encoder.encode(iFace));
            stringForm.append(ITEM_SEPARATOR_CHAR);
            stringForm.append(encoder.encode(concrete));
            stringForm.append(TYPE_TUPLE_END);
            stringForm.append(ATTRIBUTE_LIST_START);
            for(String attKey : cSpec.getAttributeKeys()){
                String val;
                try {
                    val = cSpec.getAttribute(attKey);
                    stringForm.append(encoder.encode(attKey));
                    stringForm.append(ATTRIBUTE_SPLITTER);
                    
                    if(cSpec.isContextAttribute(attKey))
                        stringForm.append(IObjectSpec.CONTEXT_VALUE);
                    else
                        stringForm.append(encoder.encode(val));
                    
                    stringForm.append(ITEM_SEPARATOR_CHAR);
                } catch (IllegalAttributeException e) {
                    throw new InternalError("IObjectSpec reported supporting an attribute " +
                            "it doesn't. Concurrent modification?");
                } catch (UninitializedAttributeException e) { }
            }
            stringForm.append(ATTRIBUTE_LIST_END);
        }
        
        /** Now the child map. */
        stringForm.append(ITEM_SEPARATOR_CHAR);
        for(Integer nodeId : childMap.keySet()){
            stringForm.append(nodeId);
            stringForm.append(NODE_SEPARATOR_CHAR);
            for(Integer childId : childMap.getClass(nodeId)){
                stringForm.append(childId);
                stringForm.append(NODE_SEPARATOR_CHAR);
            }
            stringForm.append(ITEM_SEPARATOR_CHAR);
        }
        
        /** Trims the last separator char. */
        return stringForm.substring(0, stringForm.length() - 1);
    }
   
    static class SpecToken{
        public static final Object ATTRIBUTE_TUPLE     = new Object();
        public static final Object TYPE_TUPLE          = new Object();
        public static final Object CHILD_LIST          = new Object();
        public static final Object NUMBER              = new Object();
        public static final Object UNKNOWN             = new Object();
        
        public String text;
        public Object type;
        
        public SpecToken (Object obj, String text){
            this.text = text;
            this.type = obj;
        }
    }
}
