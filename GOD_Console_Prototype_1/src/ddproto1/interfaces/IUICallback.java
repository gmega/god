/*
 * Created on Sep 27, 2004
 * 
 * file: IUICallback.java
 */
package ddproto1.interfaces;

/**
 * This interface is for querying the User Interface for dynamic behavior
 * aspects that are kept there mainly because they're set by the user at 
 * runtime. 
 * 
 * @author giuliano
 *
 */
public interface IUICallback {
    /**
     * Queries if remote stepping is activated for a given distributed
     * thread. 
     * 
     * @param vmgid node id
     * @param thexid distributed thread Universally Unique ID (UUID).
     * @return <b>true</b> if remote stepping is activated, <b>false</b>
     * otherwise.
     */
    public boolean queryIsRemoteOn(int uuid);
    /* This would be the flexible (and tedious) form for this method */
    // public boolean queryIsRemoteOn(byte [] gid, byte [] luid);
    
    public void printMessage(String s);
    
    public void printLine(String s);
}
