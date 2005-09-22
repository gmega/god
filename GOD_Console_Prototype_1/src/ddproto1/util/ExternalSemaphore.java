/*
 * Created on Jul 29, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Semaphore.java
 */

package ddproto1.util;

import ddproto1.interfaces.ISemaphore;

/**
 * This semaphore is built specifically for those who want to have
 * semaphore semantics within synchronized methods without facing a 
 * deadlock. Basically it's a semaphore that allows you to specify
 * the lock it will be using for its p and v operations.
 * 
 * 
 * @author giuliano
 *
 */
public class ExternalSemaphore implements ISemaphore{
    private int count;
    private Object lock;
    
    public ExternalSemaphore(int n, Object lock){
        this.count = n;
        this.lock = lock;
    }
    
    public void p() {
        synchronized (lock) {
            while (count == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
            count--;
        }
    }
    
    public void v(){
        synchronized (lock) {
            count++;
            lock.notify();
        }
    }
    
    public int getCount(){
        return count;
    }
    
}
