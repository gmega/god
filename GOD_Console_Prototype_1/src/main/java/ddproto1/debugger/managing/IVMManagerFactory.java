/*
 * Created on May 9, 2006
 * 
 * file: IVMManagerFactory.java
 */
package ddproto1.debugger.managing;

import com.sun.jdi.VirtualMachine;

import ddproto1.debugger.managing.identification.IGUIDManager;

public interface IVMManagerFactory extends INodeManagerFactory{

    public ILocalNodeManager getNodeManager(Byte gid);

    public Byte getGidByVM(VirtualMachine vm);

}