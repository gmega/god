/*
 * Created on Jun 5, 2006
 * 
 * file: IThreadManager.java
 */
package ddproto1.debugger.managing;

import java.util.List;

import org.eclipse.debug.core.model.IThread;

public interface IThreadManager {
    public IThread [] getThreads();
    public List <Integer>  getThreadIDList();
    public boolean         hasThreads();
    public boolean         allThreadsSuspended();
    public void            suspendAll() throws Exception;
    public void            resumeAll() throws Exception;
    public Integer         getThreadUUID(IThread tr);
    public IThread         getThread(Integer uuid);
}
