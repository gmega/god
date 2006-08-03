/*
 * Created on Sep 21, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IProtocolHook.java
 */

package ddproto1.localagent.client;

import ddproto1.exception.commons.CommException;

/**
 * @author giuliano
 *
 */
public interface IProtocolHook {
    public void pre_open(IConnection conn) throws CommException;
    public void pre_close(IConnection conn) throws CommException;
}
