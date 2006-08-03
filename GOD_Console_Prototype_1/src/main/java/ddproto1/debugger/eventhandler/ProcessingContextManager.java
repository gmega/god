/*
 * Created on Jan 11, 2005
 * 
 * file: ProcessingContextManager.java
 */
package ddproto1.debugger.eventhandler;

import ddproto1.exception.NoSuchElementError;

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
    	throws NoSuchElementError
    {
        IProcessingContext ipc = (IProcessingContext)contexts.get();
        if(ipc == null)
            throw new NoSuchElementError("No context!");
        
        return ipc;
    }
    
    public void register(IProcessingContext ipc){
        contexts.set(ipc);
    }
}
