/*
 * Created on Oct 16, 2004
 * 
 * file: EventTest.java
 */
package ddproto1.util.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.ParserException;
import ddproto1.util.commons.Event;
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
        mp.put("número usp", "3286245");
        mp.put("telefone:", "5579-9706");
        
        Event evt = new Event(mp, DebuggerConstants.CLIENT_UPCALL);
        byte [] _event = evt.toByteStream();
        
        Event other = new Event(_event);
        
        Set<String> keys1 = evt.getAttributeKeys();
        Set<String> keys2 = other.getAttributeKeys();
        
        assertTrue(keys1.size() == keys2.size());
        
        for(String key : keys1){
            assertTrue(keys2.contains(key));
            assertTrue(evt.getAttribute(key).equals(other.getAttribute(key)));
        }
    }
}
