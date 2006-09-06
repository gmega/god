/*
 * Created on 4/09/2006
 * 
 * file: INodeManagerRegistry.java
 */
package ddproto1.debugger.managing;

public interface INodeManagerRegistry {
    public ILocalNodeManager getNodeManager(Byte gid);
    public ILocalNodeManager getNodeManager(String name);
}
