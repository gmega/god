/*
 * Created on Apr 3, 2005
 * 
 * file: AbstractMultiMapTest.java
 */
package ddproto1.util.test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import ddproto1.util.collection.MultiMap;
import ddproto1.util.collection.OrderedMultiMap;
import ddproto1.util.collection.UnorderedMultiMap;

import junit.framework.TestCase;

public class AbstractMultiMapTest extends TestCase {

    private MultiMap<String, String> omp = new OrderedMultiMap<String, String>(LinkedList.class);
    private MultiMap<String, String> ump = new UnorderedMultiMap<String, String>(HashSet.class);
    
    public static void main(String[] args) {
    }

    public void setUp() throws Exception{
        super.setUp();
        this.addStuff();
    }
    
    public void addStuff(){
        omp.add("1", "1.1");
        ump.add("1", "1.1");
        omp.add("1", "1.2");
        ump.add("1", "1.2");
        omp.add("1", "1.3");
        ump.add("1", "1.3");
    }


    public void testKeySet() {
        Set <String> ompSet = omp.keySet();
        Set <String> umpSet = ump.keySet();
        assertTrue(ompSet.size() == 1);
        assertTrue(umpSet.size() == 1);
        
        Iterator it = ump.get("1").iterator();
        while(it.hasNext()){
            it.next();
            it.remove();
        }
        
        umpSet = ump.keySet();
        assertTrue(umpSet.size() == 0);
        
        it = omp.get("1").iterator();
        while(it.hasNext()){
            it.next();
            it.remove();
        }
        
        ompSet = ump.keySet();
        assertTrue(ompSet.size() == 0);
        
        this.addStuff();
    }

    public void testSize() {
        assertTrue((omp.size("1") == ump.size("1")) && (ump.size("1") == 3));
    }

    public void testContainsKey() {
        assertTrue(omp.containsKey("1") && !omp.containsKey("2"));
        assertTrue(ump.containsKey("1") && !ump.containsKey("2"));
    }

    public void testContains() {
        assertTrue(omp.contains("1", "1.1"));
        assertTrue(omp.contains("1", "1.2"));
        assertTrue(omp.contains("1", "1.3"));
        assertTrue(!omp.contains("1", "1.4"));
        assertTrue(ump.contains("1", "1.1"));
        assertTrue(ump.contains("1", "1.2"));
        assertTrue(ump.contains("1", "1.3"));
        assertTrue(!omp.contains("1", "1.4"));
    }

    public void testRemove() {
        assertTrue(omp.contains("1", "1.1") && ump.contains("1", "1.1"));
        omp.remove("1", "1.1");
        ump.remove("1", "1.1");
        assertFalse(omp.contains("1", "1.1") || ump.contains("1", "1.1"));
    }
}
