/*
 * Created on Aug 10, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IVMStateManager.java
 */

package ddproto1.debugger.managing;


import com.sun.jdi.ThreadReference;

/**
 * Java-specific version of ILocalThreadManager
 */
public interface IJavaThreadManager extends ILocalThreadManager{
    /**
     * Sets the thread passed as a parameter to be the last thread
     * in which a relevant event has occurred.
     * 
     * @param tr
     * @deprecated This made sense in the console version, but doesn't
     * make sense in the Eclipse version.
     */
    public void setCurrentThread(ThreadReference tr);
    
    /**
     * Returns the  
     * 
     * @return
     * @deprecated
     */
    public ThreadReference getCurrentThread();
    
    /**
     * Returns the JDI reference with a specific local id. This ID is JVM 
     * assigned, it might be the same for two threads belonging to different 
     * machines.   
     * 
     * @param gid The global id of the thread.
     * @return the thread reference matching the id, or <b>null</b> if
     * no such thread exists.
     */
    public ThreadReference findThreadById(long gid);
    
    /**
     * Returns a JDI reference to a thread with a specific global id.
     * This ID is globally unique.
     * 
     * @param uuid
     * @return 
     */
    public ThreadReference findThreadByUUID(int uuid);
    
    /**
     * Returns the globally unique ID of a given JDI thread.
     * 
     * @param tr
     * @return
     */
    public Integer getThreadUUID(ThreadReference tr);

    /**
     * Since com.sun.jdi.ThreadReference doesn't implement IAdaptable,
     * this method adapts ThreadReference to JavaThread. We should 
     * use PlatformObject to do this in the future and deprecate this
     * method.
     * 
     * @param tr
     * @return
     */
    public JavaThread getAdapterForThread(ThreadReference tr);
    
    /**
     * Notifies this thread manager that the Virtual Machine
     * has been suspended.
     */
    public void notifyVMSuspend();
}
