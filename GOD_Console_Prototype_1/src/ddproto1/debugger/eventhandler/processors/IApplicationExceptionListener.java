/*
 * Created on Sep 27, 2005
 * 
 * file: IApplicationExceptionListener.java
 */
package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.ObjectReference;

public interface IApplicationExceptionListener {
    public void exceptionOccurred(String cause, ObjectReference exception, int dt_uuid, int lt_uuid);
}
