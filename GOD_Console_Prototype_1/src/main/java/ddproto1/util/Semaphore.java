/*
 * Created on Aug 16, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Semaphore.java
 */

package ddproto1.util;

import ddproto1.interfaces.ISemaphore;

/** Simple implementation of classical semaphore on top of Java monitors. 
 * Do not use it inside synchronized methods or you'll deadlock for sure 
 * (for synchronized methods use an ExternalSemaphore).
 * 
 * @author giuliano
 * @see ddproto1.util.ExternalSemaphore
 * 
 * @deprecated Concurrency utils semaphore implementation is way 
 * better than this.
 *
 */
public class Semaphore implements ISemaphore {
    private static final int INFINITE = -1;
    
    int count, max;

    public Semaphore(int count){
        this(count, INFINITE);
    }
    
    public Semaphore(int count, int max){
        synchronized(this){
            this.count = count;
            this.max = max;
        }
    }
    
    public synchronized void p(){
        while(count == 0){
            try{
                wait();
            }catch(InterruptedException e) { }
        }
        count--;
    }
    
    public synchronized void v(){
        if((count <= max) || (max == INFINITE)){
            count++;
        }
        notify();
    }
    
    public synchronized int getCount(){
        return count;
    }
}
