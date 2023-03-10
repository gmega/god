/*
 * Created on Jan 30, 2005
 * 
 * file: FormatHandlerTest.java
 */
package ddproto1.util.test;

import ddproto1.util.traits.commons.ConversionUtil;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class FormatHandlerTest extends TestCase {

    public void testInt2Hex() {
        ConversionUtil fh = ConversionUtil.getInstance();
        
        int positive = 10000;
        String hexy = fh.int2Hex(positive);
        int back_positive = fh.hex2Int(hexy);
        assertTrue(back_positive == positive);
        
        int negative = -100000000;
        hexy = fh.int2Hex(negative);
        int back_negative = fh.hex2Int(hexy);
        assertTrue(back_negative == negative);
        
    }

}
