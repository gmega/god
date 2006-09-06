/*
 * Created on Jul 21, 2004
 *
 */
package ddproto1.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

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
    
    private volatile IMessageBox stderr = null;
    private volatile IMessageBox stdout = null;
    private volatile IMessageBox warning = null;
    private volatile IMessageBox debug = null;
    
    private volatile ILogManager logManager;
    
    private static final MessageHandler instance = new MessageHandler();

    private MessageHandler() { }

    public static MessageHandler getInstance() {
        return instance;
    }
    
    public synchronized static void autoConfigure(){
        getInstance().setLogManagerDelegate(
                new ILogManager(){
                    public Logger getLogger(Class c) {
                        return Logger.getLogger(c);
                    }

                    public Logger getLogger(String name) {
                        return Logger.getLogger(name);
                    }
                });
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
        Writer result = new StringWriter();
        printStackTraceOn(aThrowable, result);
        this.getErrorOutput().println(result.toString());
    }
    
    public void printStackTraceOn(Throwable aThrowable, Writer writer){
        PrintWriter printWriter = new PrintWriter(writer);
        aThrowable.printStackTrace(printWriter);
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
    
    public void logExceptions(List<Exception> exceptions){
        checkLogManager();
        logExceptions(getLogger(MessageHandler.class), exceptions);
    }
    
    public void logExceptions(Logger logger, List<Exception> exceptions){
        if(exceptions.isEmpty()) return;
        StringWriter sWriter = new StringWriter();
        for(Exception ex : exceptions){
            printStackTraceOn(ex, sWriter);
            sWriter.append('\n');
        }
        logger.error(sWriter.getBuffer().toString());
    }

}
