/*
 * Created on Sep 8, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IConfigRequestHandler.java
 */

package ddproto1.configurator;

import ddproto1.util.ByteMessage;

/**
 * @author giuliano
 *
 */
public interface IConfigRequestHandler {
    public ByteMessage handleRequest(Integer gid, ByteMessage req);
}
