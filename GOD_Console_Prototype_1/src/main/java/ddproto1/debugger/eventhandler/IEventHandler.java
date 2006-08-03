 /*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: EventHandler.java
 */

package ddproto1.debugger.eventhandler;


import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

/**
 * @author giuliano
 *
 */
public interface IEventHandler {
    /* Standard JDI events */
    public void handleVMStartEvent(VMStartEvent e);
    public void handleVMDeathEvent(VMDeathEvent e);
    public void handleVMDisconnectEvent(VMDisconnectEvent e);
    public void handleThreadStartEvent(ThreadStartEvent e);
    public void handleThreadDeathEvent(ThreadDeathEvent e);
    public void handleBreakpointEvent(BreakpointEvent e);
    public void handleExceptionEvent(ExceptionEvent e);
    public void handleStepEvent(StepEvent e);
    public void handleClassPrepareEvent(ClassPrepareEvent e);
}
