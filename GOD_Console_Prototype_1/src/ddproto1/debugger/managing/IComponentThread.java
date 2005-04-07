/*
 * Created on Sep 26, 2004
 * 
 * file: ComponentThread.java
 */
package ddproto1.debugger.managing;

import com.sun.jdi.ThreadReference;

import ddproto1.debugger.managing.tracker.DistributedThread;

/**
 * @author giuliano
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IComponentThread {
    public ThreadReference getThread();
    public DistributedThread getEncompassingDT();
    public VirtualMachineManager parentVM();       
}
