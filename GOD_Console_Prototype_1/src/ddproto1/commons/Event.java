/*
 * Created on Sep 27, 2004
 * 
 * file: ParsedEvent.java
 */
package ddproto1.commons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.IQueriableConfigurable;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.ParserException;
import ddproto1.exception.UnsupportedException;

/**
 * This class represents the events that get passed from local to global agent
 * during the debug process. It implements our default interface for conveying
 * information, the IInfoCarrier interface. It has it's addAttribute operation
 * disabled.
 * 
 * Events exhibit behavioral completeness with respect to its bytestream encoding
 * policies. Since this is a prototype, I'm doing some concept labs as well. The 
 * overall impression is that by maintaining the code to encode and decode events
 * as bytestreams inside the event class (not decoupling the parser code from the 
 * class that actually contains the data) things get a bit easier to manipulate 
 * and changes get easier to predict. 
 * 
 * @author giuliano
 */
public class Event implements IQueriableConfigurable {
    
    /* Make this smaller than 2 and get an access violation exception. */
    private static int START_LENGTH = 10;

    private Map <String, String> attmap;
    private Set<String> keys;
    private byte type;
    
    /** Produces an event from a key/value java.util.Map. Information passed onto 
     * the map will be accessible via the <code>getAttribute</code> and <code>setAttribute</code> 
     * methods of this class. The byte parameter must specify what is the <b>TYPE</b> of this 
     * event. Though no checking is performed to see wether the type passed as parameter is 
     * valid or not, you should restrict yourself to the list of server-supported types 
     * described in <code>ddproto1.commons.DebuggerConstants</code>
     * 
     * @param <b>attmap</b> - the attribute map for this event.
     * @param <b>type</b> - the event's type.
     */
    public Event(Map <String, String> attmap, byte type){
        this.attmap = (attmap == null)?new HashMap():attmap;
        keys = attmap.keySet();
                
        this.type = type;
    }
    
    /** Produces an event from a bytestream. The format for the bytestream must obey the
     * format described in the <code>ddproto1.commons.Event#toByteStream()</code> method 
     * documentation. 
     * 
     * @param _event - Event as byte stream.
     * @throws ParserException - If the byte stream does not obbey the required format.
     */
    public Event(byte [] _event)
    	throws ParserException
    {
        try{
            type = _event[0];
            
            Map <String, String>attmap = new HashMap<String, String>();
        
            /* Message starts at the first byte after the SIZE_BYTES index */
            for(int i = 1; i < _event.length; i++){
                if(_event[i] == 0) break; // Key has zero length. We're done.
                String key = read(_event, _event[i], i);
                i += (key.length() + 1);
                String val = read(_event, _event[i], i);
                i += (val.length());
                attmap.put(key, val);
             }
            
            this.attmap = attmap;
            Set <String> keySet = attmap.keySet();
            this.keys = keySet;
            
        }catch(ArrayIndexOutOfBoundsException ex){
            throw new ParserException("Message does not obey the required format.");
        }

    }
    
    /* (non-Javadoc)
     * @see ddproto1.configurator.IInfoCarrier#getAttribute(java.lang.String)
     */
    public String getAttribute(String key) throws IllegalAttributeException {
        if(!attmap.containsKey(key))
            throw new IllegalAttributeException("Invalid attribute " + key);
                
        return (String)attmap.get(key);
    }
    
    public byte getType(){
        return type;
    }
    
    public Set<String> getAttributeKeys(){
        return keys;
    }

    /** This method encodes an event as a byte stream. The format is pretty straightforward:
     * 
     * [TYPE]([key_length][key][value_length][value])*[0]
     * 
     * @return byte array with the encoded event.
     */
    public byte [] toByteStream(){
        
        byte body [] = new byte[START_LENGTH];
        
        body[0] = type;
        int used = 1;

        Set s = attmap.keySet();
        Iterator it = s.iterator();
        
        if(s.size() > 255){
            throw new UnsupportedException("Events cannot have more than 255 pairs.");
        }
        
        while(it.hasNext()){
            String key = (String)it.next();
            byte [] _key = key.getBytes();
            byte [] _val = ((String)attmap.get(key)).getBytes();

            int required = (used + _key.length + 1 + _val.length + 1);
            
            if(required >= body.length){
                /* Doubling yields constant amortized insertion cost, even with
                 * the array copy. */
                byte [] body_aux = new byte[body.length*2];
                while(required >= body_aux.length)
                    body_aux = new byte[body_aux.length*2];
                
                System.arraycopy(body, 0, body_aux, 0, used);
                body = body_aux;
            }
            
            body[used++] = (byte)_key.length;
            System.arraycopy(_key, 0, body, used, _key.length);
            used += _key.length;
            body[used++] = (byte)_val.length;
            System.arraycopy(_val, 0, body, used, _val.length);
            used += _val.length;
        }
        
        return body;
    }
    
    private String read(byte [] body, int length, int offset){
        byte[] word = new byte[length]; 
        System.arraycopy(body, offset+1, word , 0, length);
        return new String(word);
    }

    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
        throw new UnsupportedOperationException();
    }

    public boolean isWritable() {
        // TODO Auto-generated method stub
        return false;
    }
}    

