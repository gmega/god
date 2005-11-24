/*
 * Created on Aug 10, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IVMStateManager.java
 */

package ddproto1.debugger.managing;

import java.util.List;

import org.eclipse.debug.core.model.IThread;

import com.sun.jdi.ThreadReference;

/**
 * @author giuliano
 *
 * This interface is going through a process of conversion to the 
 * Eclipse debug model, which is language-independent. 
 *
 */
public interface IVMThreadManager {

    /** Those methods should be converted to use IThreads only. */
    public void setCurrentThread(ThreadReference tr);
    public ThreadReference findThreadById(long uid);
    public ThreadReference findThreadByUUID(int uuid);
    public ThreadReference findThreadByUUID(Integer uuid);
    public ThreadReference getCurrentThread();

    /** Those methods are language-independent. */
    public IThread []      getThreads();
    public List <Integer>  getThreadIDList();
    public Integer         getThreadUUID(ThreadReference tr);
    public boolean         hasThreads();
    public boolean         allThreadsSuspended();
    public void            suspendAll() throws Exception;
    public void            resumeAll() throws Exception;
    
    /** Should be moved to a IJavaThreadManager extends IVMThreadManager interface. */
    public JavaThread      getLIThread(ThreadReference tr);
    public void            notifyVMSuspend();
    
}
