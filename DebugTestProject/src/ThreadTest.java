import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * Created on Jan 29, 2006
 * 
 * file: ThreadTest.java
 */

public class ThreadTest {
    
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(false);
    
    public static void main(String[] args){
        ThreadTest tt = new ThreadTest();
        tt.doIt();
    }
    
    public void doIt(){
        Thread t1 = new Thread(new Reader());
        Thread t2 = new Thread(new Writer());
        t1.start();
        t2.start();
        
    }
    
    private class Reader implements Runnable{

        public void run() {
            try{
                rwLock.readLock().lock();
                rwLock.readLock().lock();
                rwLock.readLock().unlock();
            }finally{
                rwLock.readLock().unlock();
            }
        }
        
    }
    
    private class Writer implements Runnable{

        public void run() {
            rwLock.writeLock().lock();
            rwLock.writeLock().unlock();
        }
        
    }
}
