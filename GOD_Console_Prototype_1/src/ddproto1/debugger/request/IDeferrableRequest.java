/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IDeferrableRequest.java
 */

package ddproto1.debugger.request;

import java.util.List;

import ddproto1.exception.IllegalStateException;

/**
 * Deferrable Requests are, as their name implies, requests that could get
 * deferred until some set of preconditions is satisfied. Classes implementing
 * interface IDeferrableRequest normally need to register themselves with the
 * runtime environment, which will make them aware of which preconditions have
 * been met and which haven't. An IDeferrableRequest must advertise it's required
 * preconditions through method <b>getRequirements</b>. Supported precondition 
 * types are, as of now:
 * 
 * <ol>
 * <li><b> IDeferrableRequest.CLASSLOADING</b> satisfied when the class
 * loaded matches the one returned in IPrecondition#getClassId().</li>
 * 
 * <li><b> IDeferrableRequest.VM_CONNECTION</b> satisfied right at the time 
 * when the debugger attaches to the JVM and before any events are processed. </li>
 * 
 * <li><b> IDeferrableRequest.THREAD_PROMOTION</b> satisfied when the local thread with
 * dotted ID as returned by IPrecondition#getClassId() gets promoted to distributed
 * thread. </li>
 * 
 * <li><b> IDeferrableRequest.NIL</b> nil precondition. Always satisfied by default. </li>
 * 
 * </ol>
 * 
 * 
 * 
 * 
 * @author giuliano
 *
 */

public interface IDeferrableRequest {

    public static final int CLASSLOADING = 0;
    public static final int VM_CONNECTION = 1;
    public static final int THREAD_PROMOTION = 2; 
    public static final int NIL = -1;
    
    public static final int MATCH_ONCE = 0;
    public static final int MATCH_MULTIPLE = 1;
    
    public static final Object REQUEST_CANCELLED = new Object();
    public static final Object REQUEST_RESOLVED  = new Object();
    public static final Object OK                = new Object();
        
    /** Resolve now is more like a firm attempt at resolving a request.
	 * Normally requests require some sort of context from where to access
	 * precondition validators or subproducts in order to be able to resolve
	 * themselves. 
     * 
     * This interface defines an implicit protocol for resolution communication.
     * Resolution is conceptually broken into several phases by the preconditions.
     * At each fase (each resolveNow call), the underlying deferrable request should:
     * 
     * 1 - Return null if it couldn't solve this fase of its own resolution, even
     *     after the precondition has been met.
     *     
     * 2 - Throw an exception if an error occurs.
     * 
     * Note that those two are different. The first one indicates some sort of logic/
     * configuration error, or even a wrong precondition advertisement. The second 
     * indicates a runtime error which might or might not be due to the first error. 
     * Though the second error is more general, the first one is more serious and
     * therefore some effort must be done to detect it and follow the protocol.
	 * 
	 * <b>Note:</b> ClassCastExceptions may occur if the object passed as 
	 * context is of the wrong type.
     * 
     * 16/05/2005 - 
     * 
     * The above protocol is fine, but it doesn't take into consideration some
     * possible outcomes. The request might have been cancelled, and there are times
     * where it simply makes no sense to return anything, so there are DeferrableRequests
     * that simply create an object and return it. Originally, the return value would 
     * contain the request when it could be resolved eagerly, but now the non-null value
     * means a given phase in resolution was carried out successfully.
     * 
     * Everything cries out for return codes instead of null/non-null return values. 
     *
     * Therefore, I'm adding return codes. The products of deferrable request resolution
     * should be retrieved by registered listeners, which was the way things were being 
     * done already anyway.
     * 
	 * 
	 * @param rt
	 * @return
	 */
    public Object resolveNow(IResolutionContext context) throws Exception;
    
    /** Allows access to resolution preconditions. It should be a fixed list
     * of preconditions and should not vary during the life of the deferrable
     * request. Support for some types of dynamic preconditions exist, but this
     * is not the place to do it.
     */
    public List <IPrecondition> getRequirements();
    
    public void addResolutionListener(IResolutionListener listener);
    
    public void removeResolutionListener(IResolutionListener listener);
    
    /**
     * This method cancels the underlying event request if it has been fulfilled, or
     * precludes it from being fulfilled if it hasn't.
     * 
     * @throws Exception
     */
    public void cancel() throws Exception;
    
    public boolean isCancelled();
    
    /** This interface serves as a mean of communicating concrete DeferrableRequest
	 * implementations preconditions to the outside world.<BR>
	 * 
	 * Though there are no guarantees as to what those methods will actually return,
	 * the recommended behaviour is described here.
	 * 
	 * @author giuliano
	 *
	 */
	interface IPrecondition{
	    /** Should return the precondition type for this DeferrableRequest.
	     * 
	     * @return valid values are <b>DeferrableRequest.CLASSPREPARE</b> and
	     *  <b>DeferrableRequest.VM_CONNECTION</b>.
	     */
	    public IPreconditionType getType();
	    
	    /** Returns the (possibly unqualified) name of the class that fulfills
	     * the precondition for this DeferrableRequest when loaded.
	     * 
	     */
	    public String getClassId();
	}
    
    interface IPreconditionType {
        public int eventType();
        public int matchType();
    }
	
	
	/** Simple interface that serves as an information carrier for resolving requests.
	 * 
	 * @author giuliano
	 *
	 */
	
	interface IResolutionContext{
	    public IPrecondition getPrecondition();
	    public Object getContext();
	}
}