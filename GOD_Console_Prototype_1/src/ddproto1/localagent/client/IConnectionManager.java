/*
 * Created on Sep 21, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IConnectionManager.java
 */

package ddproto1.localagent.client;

import ddproto1.exception.commons.CommException;


/**
 * @author giuliano
 *
 */
public interface IConnectionManager {
    public IConnection acquire() throws CommException;
    public void setProtocolHook(IProtocolHook iph);
    public void closeAll() throws CommException;
}
