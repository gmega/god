/*
 * Created on Sep 8, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IConfigRequestHandler.java
 */

package ddproto1.debugger.server;

import ddproto1.util.commons.ByteMessage;

/**
 * @author giuliano
 *
 */
public interface IRequestHandler {
    public ByteMessage handleRequest(Byte gid, ByteMessage req);
}
