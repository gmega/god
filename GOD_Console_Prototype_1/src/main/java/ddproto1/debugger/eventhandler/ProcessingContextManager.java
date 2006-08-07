/*
 * Created on Jan 11, 2005
 * 
 * file: ProcessingContextManager.java
 */
package ddproto1.debugger.eventhandler;

import ddproto1.exception.NoContextException;

/**
 * @author giuliano
 *
 */
public class ProcessingContextManager {
    private static ProcessingContextManager instance;
    
    private ThreadLocal contexts = new ThreadLocal();
    
    private ProcessingContextManager() { }
    
    public synchronized static ProcessingContextManager getInstance(){
        return (instance == null)?instance = new ProcessingContextManager():instance;
    }
    
    public IProcessingContext getProcessingContext()
    {
        return (IProcessingContext)contexts.get();
    }
    
    public void register(IProcessingContext ipc){
        contexts.set(ipc);
    }
}
