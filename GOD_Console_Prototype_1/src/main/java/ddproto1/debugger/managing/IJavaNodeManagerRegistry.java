/*
 * Created on 4/09/2006
 * 
 * file: IJavaNodeManagerRegistry.java
 */
package ddproto1.debugger.managing;

import com.sun.jdi.VirtualMachine;

public interface IJavaNodeManagerRegistry extends INodeManagerRegistry {
    public IJavaNodeManager getJavaNodeManager(String name);
    public IJavaNodeManager getJavaNodeManager(Byte gid);
    public IJavaNodeManager getJavaNodeManager(VirtualMachine vm);
}
