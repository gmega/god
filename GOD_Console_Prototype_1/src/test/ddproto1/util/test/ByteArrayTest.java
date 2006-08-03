/*
 * Created on Sep 10, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ByteArrayTest.java
 */

package ddproto1.util.test;

import ddproto1.util.CharByteArray;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class ByteArrayTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ByteArrayTest.class);
    }

    public void testWriteAt() {
        CharByteArray barray = new CharByteArray(5);
        barray.writeAt(0, (byte)1);
        barray.writeAt(1, (byte)2);
        barray.writeAt(2, (byte)3);
        barray.writeAt(3, (byte)255);
        barray.writeAt(4, (byte)255);
        
        assertTrue(barray.get(0) == 1);
        assertTrue(barray.get(1) == 2);
        assertTrue(barray.get(2) == 3);
        assertTrue(barray.get(3) == (byte)255);
        assertTrue(barray.get(4) == (byte)255);
        
        barray = barray.insert(new byte[] {1, 3, 5}, 3);
        
        assertTrue(barray.get(3) == 1);
        assertTrue(barray.get(4) == 3);     
        assertTrue(barray.get(5) == 5);
        assertTrue(barray.get(6) == (byte)255);
        assertTrue(barray.get(7) == (byte)255);
    }

}
