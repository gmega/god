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
 * REMARK Don't know if this interface will need any changes.
 */
public interface IVMThreadManager {

	public boolean isVMSuspended();
    public void suspendAll() throws Exception;
    public void resumeAll() throws Exception;

    /** Those methods should be converted to use IThreads only. */
    public void setCurrentThread(ThreadReference tr);
    public ThreadReference findThreadById(long uid);
    public ThreadReference findThreadByUUID(int uuid);
    public ThreadReference findThreadByUUID(Integer uuid);
    public ThreadReference getCurrentThread();
    public List<IThread> getThreadIDList();

    public Integer         getThreadUUID(ThreadReference tr);
    
    /** Should be moved to a IJavaThreadManager */
    public JavaThread      getLIThread(ThreadReference tr);
}
