/*
 * Created on Jun 5, 2006
 * 
 * file: IThreadManager.java
 */
package ddproto1.debugger.managing;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IThread;

public interface IThreadManager extends IAdaptable{
    public IThread[]    getThreads();
    public boolean      hasThreads();
    public Integer      getThreadUUID(IThread tr);
    public IThread      getThread(Integer uuid);
}
