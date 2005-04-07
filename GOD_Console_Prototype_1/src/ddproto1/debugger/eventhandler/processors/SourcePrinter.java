/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SourcePrinter.java
 */

package ddproto1.debugger.eventhandler.processors;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.LocatableEvent;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.eventhandler.IEventManager;
import ddproto1.debugger.eventhandler.IProcessingContext;
import ddproto1.debugger.eventhandler.ProcessingContextManager;
import ddproto1.interfaces.IMessageBox;
import ddproto1.sourcemapper.ISourceMapper;

/**
 * @author giuliano
 *
 */
public class SourcePrinter extends BasicEventProcessor{

    public static final String NO_SOURCE = "NO_SOURCE";
    
    private static final ProcessingContextManager pcm = ProcessingContextManager.getInstance();
    
    private ISourceMapper sm;
    private IMessageBox mb;
    
    public SourcePrinter(ISourceMapper sm, IMessageBox mb){
        this.sm = sm;
        this.mb = mb;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.EventListener#process(com.sun.jdi.event.Event)
     */
    public synchronized void specializedProcess(Event e) {
        
        IProcessingContext ctx = pcm.getProcessingContext();
        
        /* Only prints info if the VM is going to halt... */
        if(ctx.getResults(IEventManager.RESUME_SET) > 0)
            return;
        
        /* ... and if there's no explicit requested not to. 
         * 
         * We have to catch the exception because of the way
         * the voting structure is designed. Whomever is interested
         * in voting for this particular vote type should declare
         * it though interface IVotingManager, however, if nobody
         * is interested, the vote type is never registered. In that
         * case, we'll get a runtime exception when trying to inpect it.
         * */
        try{
            if(ctx.getResults(SourcePrinter.NO_SOURCE) > 0)
                return;
        }catch(RuntimeException ex){}
                    
        // Synchronization ensures no information scrambling will occur.
        LocatableEvent le = (LocatableEvent)e;
        String line = sm.getLine(le.location());
        
        int number = le.location().lineNumber();
        String src = "<no source info available>";
        String machine = (String)e.request().getProperty(DebuggerConstants.VMM_KEY);
        if(machine == null) machine = "Unknown machine";
        String dt = (String)e.request().getProperty("dtid");
        String lt = (String)e.request().getProperty("ltid");
        String hex = (String)e.request().getProperty("hexid");
        
        try{ 
            src = le.location().sourceName();
        }catch(AbsentInformationException ex) { }
        
        String msg = ((dt == null)?"Non-distributed":"Distributed") + 
        				" thread ";
        
        if(dt != null){
            msg += " [Global ID:"+ dt + "],"; 
        }
        
        if(lt != null){
            msg += "[Local ID:" + lt +"],";
        }
        
        msg += ((hex != null)?"[Hex ID:"+ hex +"]":"[unknown vm id]");
        msg += "\n    stopped at <" + machine + ">, " + src + ":";
        msg += "\n[" + number + "]:" + line;
        mb.println(msg);
    }

}
