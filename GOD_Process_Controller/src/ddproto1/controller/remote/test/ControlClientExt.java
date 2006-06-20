/*
 * Created on Jun 19, 2006
 * 
 * file: ControlClientExt.java
 */
package ddproto1.controller.remote.test;

import ddproto1.controller.client.ControlClientPrx;

public interface ControlClientExt extends ControlClientPrx {
    public boolean isDone();
}
