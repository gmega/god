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
 * @author giuliano
 *
 * This interface is going through a process of conversion to the 
 * Eclipse debug model, which is language-independent. 
 *
 */
public interface IJavaThreadManager extends ILocalThreadManager{

    /** Those methods should be converted to use IThreads only. */
    public void setCurrentThread(ThreadReference tr);
    public ThreadReference findThreadById(long uid);
    public ThreadReference findThreadByUUID(int uuid);
    public ThreadReference findThreadByUUID(Integer uuid);
    public ThreadReference getCurrentThread();
    public Integer         getThreadUUID(ThreadReference tr);
    public JavaThread      getLIThread(ThreadReference tr);
    public void            notifyVMSuspend();

    
}
