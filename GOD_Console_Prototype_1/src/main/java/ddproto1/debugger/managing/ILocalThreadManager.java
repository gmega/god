/*
 * Created on Jun 10, 2006
 * 
 * file: ILocalThreadManager.java
 */
package ddproto1.debugger.managing;

import ddproto1.debugger.managing.tracker.ILocalThread;

public interface ILocalThreadManager extends IThreadManager{
    public ILocalThread [] getLocalThreads();
    public ILocalThread    getLocalThread(Integer uuid);
}
