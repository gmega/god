/*
 * Created on Feb 1, 2005
 * 
 * file: LockingHashMapTest.java
 */
package ddproto1.util.test;

import ddproto1.util.collection.LockingHashMap;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class LockingHashMapTest extends TestCase {

    private LockingHashMap mp = new LockingHashMap();

    public void testLockForReading() {
        Thread tr = new Thread(new Reader());
        Thread tw = new Thread(new Writer());
        tr.start();
        tw.start();
    }
    
    private class Reader implements Runnable {
        public void run() {
            try {
                mp.lockForReading();
                try {
                    synchronized (this) {
                        this.wait(1000);
                    }
                } catch (InterruptedException e) {
                }
                boolean b = mp.isEmpty();
                for (int i = 0; i < 10; i++) {
                    assertTrue(b = mp.isEmpty());
                }
                mp.unlockForReading();
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }

        }

    }
    
    private class Writer implements Runnable{
        public void run(){
            try{
                mp.lockForWriting();
                mp.put("lixo", "lixo");
                mp.unlockForWriting();
            }catch(Exception e){
                e.printStackTrace();
                fail();
            }
        }
    }

}
