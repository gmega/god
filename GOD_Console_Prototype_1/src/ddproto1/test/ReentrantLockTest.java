/*
 * Created on Sep 23, 2005
 * 
 * file: ReentrantLockTest.java
 */
package ddproto1.test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantLockTest implements Runnable{
    
    private static ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private static Lock rLock = rwLock.readLock();
    private static Lock wLock = rwLock.writeLock();
    
    private boolean write;
    
    public static void main(String [] args){
        Thread t1 = new Thread(new ReentrantLockTest(false));
        Thread t2 = new Thread(new ReentrantLockTest(true));
        
        t1.start();
        synchronized(t2){ 
            try{ t2.wait(2000); } catch (InterruptedException ex) { }
        }
        t2.start();
    }
    
    private ReentrantLockTest(boolean write){
        this.write = write;
    }

    public void run() {
        if(write){
            wLock.lock();
            wLock.unlock();
        }
        else{
            rLock.lock();
            synchronized(this){
                try{ this.wait(5000); } catch (InterruptedException ex) { }
            }
            rLock.lock();
            rLock.unlock();
            rLock.unlock();
        }
    }
    
}
