/*
 * Created on Sep 20, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IGlobalAgent.java
 */

package ddproto1.localagent.client;

import ddproto1.commons.Event;
import ddproto1.exception.CommException;


/**
 * @author giuliano
 *
 */
public interface IGlobalAgent {
    public String getAttribute(String key) throws CommException;
    public int syncNotify(Event e) throws CommException;
    public void asyncNotify(Event e) throws CommException;
    public void dispose() throws CommException;
}
