/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ResolutionContext.java
 */

package ddproto1.debugger.request;

import ddproto1.debugger.request.IDeferrableRequest.IPrecondition;
import ddproto1.debugger.request.IDeferrableRequest.IResolutionContext;

/**
 * @author giuliano
 *
 */
public class StdResolutionContextImpl implements IResolutionContext {

    private IPrecondition prec;
    private Object context;

    public void setPrecondition(IPrecondition ip){
        this.prec = ip;
    }
    
    public void setContext(Object ctx){
        this.context = ctx;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest.IResolutionContext#getPrecondition()
     */
    public IPrecondition getPrecondition() {
        return prec;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest.IResolutionContext#getContext()
     */
    public Object getContext() {
        return context;
    }

}
