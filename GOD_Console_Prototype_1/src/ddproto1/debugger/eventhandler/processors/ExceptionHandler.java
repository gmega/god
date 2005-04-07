/*
 * Created on Aug 9, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ExceptionHandler.java
 */

package ddproto1.debugger.eventhandler.processors;

import java.util.ArrayList;

import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ExceptionEvent;

import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 */
public class ExceptionHandler extends BasicEventProcessor{
    
    private static MessageHandler mh = MessageHandler.getInstance();

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.EventProcessor#specializedProcess(com.sun.jdi.event.Event)
     */
    protected void specializedProcess(Event e) {
        ExceptionEvent ee = (ExceptionEvent) e;
        
        Location l = ee.catchLocation();
        
        if(l == null) l = ee.location();

        String stacktrace = "";
        
        try{
            stacktrace += getStackTrace(ee);
        }catch(IncompatibleThreadStateException itse){
            mh.getStandardOutput().println("Could not obtain thread's call stack since " +
            		"it appears not to have been stopped");
        }
       
        if (l == null){
            mh.getStandardOutput().println(
                    "Uncaught exception " + ee.exception() + " in machine "
                            + dc.getProperty("name") + "." + "\n" + stacktrace);
            
        }else{
            mh.getStandardOutput().println(
                    "Caught exception " + ee.exception() + " in machine "
                            + dc.getProperty("name") + "\n" + " at " + l + ".\n" + stacktrace);
        }
    }
    
    private String getStackTrace(ExceptionEvent ee)
    	throws IncompatibleThreadStateException
    {
        /* Prints a short (hopefully helpful) description for this exception */
        ObjectReference or = ee.exception();
        StringReference str = null;
        try{
            Method reason = (Method)or.referenceType().methodsByName("getMessage").iterator().next();
            str = (StringReference)or.invokeMethod(ee.thread(), reason, new ArrayList(), ClassType.INVOKE_SINGLE_THREADED);
        }catch(Exception e){ 
            mh.getStandardOutput().println("Unable to determine the cause of the exception.");
        }
        
        String mesg = "Reason: " + ((str == null)?"unknown":str.value()) + 
						"\n** Stack trace:\n";
        
        ThreadReference te = ee.thread();
        for(int i = 0; i < te.frameCount(); i++){
            StackFrame stkf = te.frame(i);
            Location loc = stkf.location();
            Method m = loc.method();
            ReferenceType where = loc.declaringType();
            mesg += i + " at "+where.name()+"." + loc.method().name() + "()\n";
        }
        
        return mesg;
    }
}
