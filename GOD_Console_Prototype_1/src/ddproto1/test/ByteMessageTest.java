/*
 * Created on Jan 26, 2005
 * 
 * file: ByteMessageTest.java
 */
package ddproto1.test;

import ddproto1.util.commons.ByteMessage;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class ByteMessageTest extends TestCase {

    public static void main(String[] args) {
    }

    public void testGetSize() {
        ByteMessage bm = new ByteMessage(160);
        assertTrue(bm.getSize() == 160);
    }

}
