/*
 * Created on Sep 22, 2005
 * 
 * file: IHeadCounter.java
 */
package ddproto1.debugger.managing.tracker;

import ddproto1.exception.IllegalStateException;

/**
 * @deprecated
 * @author giuliano
 *
 */
public interface IHeadCounter {
    public int headFrameCount(int uuid) throws IllegalStateException;
    public int headFrameCount(int uuid, boolean suspend) throws IllegalStateException;
}
