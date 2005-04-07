/*
 * Created on Oct 16, 2004
 * 
 * file: EventTest.java
 */
package ddproto1.test;

import java.util.HashMap;
import java.util.Map;

import ddproto1.commons.DebuggerConstants;
import ddproto1.commons.Event;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.ParserException;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class EventTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EventTest.class);
    }

    /*
     * Class under test for void Event(byte[])
     */
    public void testEventbyteArray() 
    	throws ParserException, IllegalAttributeException
    {
        Map mp = new HashMap();
        mp.put("nome", "Giuliano");
        mp.put("idade", "22");
        mp.put("sobrenome", "Mega");
        mp.put("numerão uspão", "3286245");
        mp.put("telefone:", "5579-9706");
        
        Event evt = new Event(mp, DebuggerConstants.CLIENT_UPCALL);
        byte [] _event = evt.toByteStream();
        
        Event other = new Event(_event);
        
        String [] keys1 = evt.getAttributeKeys();
        String [] keys2 = other.getAttributeKeys();
        
        assertTrue(keys1.length == keys2.length);
        
        for(int i = 0; i < keys1.length; i++){
            assertTrue(keys1[i].equals(keys2[i]));
            assertTrue(evt.getAttribute(keys1[i]).equals(other.getAttribute(keys1[i])));
        }
    }
}
