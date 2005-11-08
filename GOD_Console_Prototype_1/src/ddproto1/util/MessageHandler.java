/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import ddproto1.exception.HandlerException;
import ddproto1.interfaces.IMessageBox;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MessageHandler implements ILogManager{

    private static final String module = "MessageHandler -";
    
    private IMessageBox stderr = null;
    private IMessageBox stdout = null;
    private IMessageBox warning = null;
    private IMessageBox debug = null;
    
    private ILogManager logManager;
    
    private static MessageHandler instance;

    private MessageHandler() { }

    public synchronized static MessageHandler getInstance() {
        if (instance == null) instance = new MessageHandler();

        return instance;
    }

    /* REMARK I suppose I could have made System.out and System.err the default
     * routes for stdout and stderr instead of forcing the user to set them up
     * or face a RuntimeException, but I don't like the idea of doing things
     * behind people's backs. I rather have people dealing with exceptions and
     * knowing what they're doing than having them fixing difficult bugs because
     * of unexpected (and insidiously put) default values.  
     */
    
    /** Returns the preset error output.
     * 
     * @return
     */
    public IMessageBox getErrorOutput() {
        if(stderr == null)
            throw new HandlerException(module + " Error - Standard error output has not been set.");
        return stderr;
    }

    /** Returns the preset standard output.
     * 
     * @return
     */
    public IMessageBox getStandardOutput() {
        if(stdout == null)
            throw new HandlerException(module + " Error - Standard output has not been set.");
        return stdout;
    }
    
    public IMessageBox getWarningOutput() {
        if(warning == null)
            throw new HandlerException(module + " Error - Warning channel has not been set.");
        
        return warning;
    }	
    
    public IMessageBox getDebugOutput() {
        if(debug == null)
            throw new HandlerException(module + " Error - Debug channel has not been set.");
        
        return debug;
    }
    
    public void setErrorOutput(IMessageBox mb){
        stderr = mb;
    }
    
    public void setStandardOutput(IMessageBox mb){
        stdout = mb;
    }
    
    public void setWarningOutput(IMessageBox mb){
        warning = mb;
    }
    
    public void setDebugOutput(IMessageBox mb){
        debug = mb;
    }

    public void printStackTrace(Throwable aThrowable) {
        if (stderr == null) return;
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        this.getErrorOutput().println(result.toString());
    }
    
    public void setLogManagerDelegate(ILogManager man){
        logManager = man;
    }
    
    /** New API. I'm deprecating all the messagebox stuff in favor of
     * using LOG4J.
     */
    public Logger getLogger(Class c) {
        checkLogManager();
        return logManager.getLogger(c);
    }

    public Logger getLogger(String name) {
        checkLogManager();
        return logManager.getLogger(name);
    }
    
    private void checkLogManager(){
        if(logManager == null)
            throw new HandlerException("Log manager hasn't been set.");
    }
}
