/*
 * Created on Aug 10, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IVMStateManager.java
 */

package ddproto1.debugger.managing;

import java.util.List;

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
    
    public List getThreadIDList();
    public void setCurrentThread(ThreadReference tr);
    public ThreadReference getCurrentThread();
    
    public ThreadReference findThreadById(long uid);
    public ThreadReference findThreadByUUID(int uuid);
    public ThreadReference findThreadByUUID(Integer uuid);
    public Integer         getThreadUUID(ThreadReference tr);
}
