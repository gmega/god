/*
 * Created on Aug 16, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: UtilityTestCase.java
 */

package ddproto1.util.test;

import ddproto1.util.traits.commons.ConversionUtil;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class UtilityTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(UtilityTest.class);
    }

    public void testToHex() {
        ConversionUtil sh = ConversionUtil.getInstance();
        long num1 = 3423234;
        String hex = sh.long2Hex(num1);
        long num2 = sh.hex2Long(hex);
        assertTrue(num1 == num2);
    }

}
