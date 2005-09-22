/*
 * Created on Sep 21, 2005
 * 
 * file: ConversionTest.java
 */
package ddproto1.test;

import ddproto1.util.traits.commons.ConversionTrait;
import junit.framework.TestCase;

public class ConversionTest extends TestCase {
    public void testConversion(){
        ConversionTrait ct = ConversionTrait.getInstance();
        
        String radical = "123";
        
        for(int i = 0; i < 256; i++){
            int uuid = ct.dotted2Uuid(i + "." + radical);
            byte gid = ct.guidFromUUID(uuid);
            ConversionTest.assertTrue(gid == (byte)i);
        }
    }
}
