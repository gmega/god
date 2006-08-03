/*
 * Created on Aug 16, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ISemaphore.java
 */

package ddproto1.interfaces;

/**
 * @author giuliano
 *
 */
public interface ISemaphore {
    /**
     * Semaphore down operation.
     *
     */
    public void p();
    /**
     * Semaphore up operation.
     *
     */
    public void v();
    
    public int getCount();
}
