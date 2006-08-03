/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DebugContext.java
 */

package ddproto1.debugger.managing;

import ddproto1.debugger.eventhandler.IVotingManager;

/**
 * @author giuliano
 *
 */
public interface IDebugContext {
    public String getProperty(String pname);
    public VirtualMachineManager getVMM();
    public IVotingManager getVotingManager();
}
