/*
 * Created on Nov 28, 2005
 * 
 * file: CircularStringBufferTest.java
 */
package ddproto1.launcher.test;

import ddproto1.launcher.CircularStringBuffer;
import junit.framework.TestCase;

public class CircularStringBufferTest extends TestCase {

    /*
     * Test method for 'ddproto1.launcher.CircularStringBuffer.append(String)'
     */
    public void testAppend() {
        CircularStringBuffer csb = new CircularStringBuffer(5);
        csb.append("Bu�as");
        assertTrue(csb.toString().equals("Bu�as"));
        csb.append("Ol�, meu nome � Giuliano Mega");
        assertTrue(csb.toString().equals(" Mega"));
        csb.append("D930C");
        assertTrue(csb.toString().equals("D930C"));
        csb.append("orno");
        assertTrue(csb.toString().equals("Corno"));
        
    }

}
