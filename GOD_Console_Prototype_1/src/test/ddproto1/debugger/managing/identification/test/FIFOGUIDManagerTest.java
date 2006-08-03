/*
 * Created on Feb 8, 2006
 * 
 * file: FIFOGUIDManagerTest.java
 */
package ddproto1.debugger.managing.identification.test;

import ddproto1.debugger.managing.identification.FIFOGUIDManager;
import ddproto1.debugger.managing.identification.IGUIDManager;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.ResourceLimitReachedException;
import junit.framework.TestCase;

public class FIFOGUIDManagerTest extends TestCase {
    public void testFIFOManager() throws Exception{
        IGUIDManager gManager = new FIFOGUIDManager(7, 2);
        Object [] users = new Object[6];
        
        /** Leases the 5 first ids (0-4) and attempts
         * to lease a sixth.
         */
        for(int i = 0; i <= 5; i++){
            users[i] = new Object();
            try{
                gManager.leaseGUID(users[i]);
            }catch(ResourceLimitReachedException ex){
                assertTrue(i == 5);
                break;
            }
            assertFalse(i == 5);            
        }
        
        /** Releases the ID of third user and reacquires.
         * By queue order, the fifth ID should be leased.
         */
        gManager.releaseGUID(users[3]);
        assertTrue(gManager.leaseGUID(users[3]) == 5);
        
        gManager.releaseGUID(users[3]);
        /** Tries to acquire an already used ID. */
        try{
        	gManager.leaseGUID(users[3], 0);
        	fail();
        }catch(DuplicateSymbolException ex){ }
        
        gManager.releaseGUID(users[3]);
        assertTrue(gManager.leaseGUID(users[3]) == 6);
        
        /** Tries to acquire a second ID. */
        try{
        	gManager.leaseGUID(users[3]);
        	fail();
        }catch(DuplicateSymbolException ex){ }
        
        gManager.releaseGUID(users[3]);
        /** Tries to acquire a constrained ID */
        try{
        	gManager.leaseGUID(users[3], 6);
        	fail();
        }catch(NoSuchSymbolException ex) { }
        
        /** Third id should've reached the top by now and
         * should be recycled.
         */
        assertTrue(gManager.leaseGUID(users[3]) == 3);


    }
}
