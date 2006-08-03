/*
 * Created on Aug 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IEventProcessor.java
 */

package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.event.Event;

/**
 * This interface specifies the basic event processor interface
 * for JDI events.
 * 
 * @author giuliano
 *
 */
public interface IJDIEventProcessor {
    public void process(Event e);
    public void setNext(IJDIEventProcessor iep);
    public IJDIEventProcessor getNext();
    public void enable(boolean status);
}
