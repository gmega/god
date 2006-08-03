/*
 * Created on Sep 21, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IConnection.java
 */

package ddproto1.localagent.client;

import ddproto1.util.commons.ByteMessage;
import ddproto1.exception.commons.CommException;

/**
 * @author giuliano
 *
 */
public interface IConnection {
    public void send(ByteMessage bm) throws CommException;
    public ByteMessage recv(int timeout) throws CommException;
    public void release();
    public boolean validate();
}
