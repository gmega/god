/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DeferrableEventRequestQueue.java
 */

package ddproto1.debugger.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;

import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.OrderedMultiMap;

/**
 * This class matches resolution contexts into preconditions. When a deferrable request is inserted
 * onto this "queue", the queue tries to resolve it by checking whether its preconditions have or have 
 * not been met. If it can't solve the request (because it's preconditions haven't been met), the request
 * is stashed until all of it's unresolved preconditions are met. The "environment" is responsible for 
 * notifying the queue of resolved preconditions. The queue will, in turn, pump the resolution to all
 * interested requests.
 * 
 * There are a few things to remember when designing deferrable requests that will be inserted here:
 * 
 * 1 - The precondition list is fixed. Once you've inserted a deferrable request into the queue, you should 
 *     NOT change its advertised preconditions (or queue assumptions will go all wrong).
 * 
 * 2 - You must obey the resolution protocol for resolveNow (@see ddproto1.debugger.request.IDeferrableRequest)
 * 
 * @author giuliano
 * 
 *
 */
public class DeferrableRequestQueue {
    
    public static final int ALLOW_DUPLICATES = 0;
    public static final int DISALLOW_DUPLICATES = 1;
    public static final int WARN_DUPLICATES = 2;
    
    private static final MessageHandler mh = MessageHandler.getInstance();
    private static final byte STATIC = 1;
    private static final byte DYNAMIC = 2;
    
    /* This map keeps deferred MATCH_ONCE requests, separated by their unresolved preconditions */
    private OrderedMultiMap<IDeferrableRequest.IPrecondition, IDeferrableRequest> matchOnce2request = 
            new OrderedMultiMap<IDeferrableRequest.IPrecondition, IDeferrableRequest>(LinkedList.class);
    
    /* This map keeps deferred MATCH_MULTIPLE requests, separated by their unresolved preconditions */
    private OrderedMultiMap<IDeferrableRequest.IPrecondition, IDeferrableRequest> matchMultiple2request = 
            new OrderedMultiMap<IDeferrableRequest.IPrecondition, IDeferrableRequest> (LinkedList.class);
    
    /* This map keeps all preconditions that have been resolved, separated by event */
    private OrderedMultiMap<IDeferrableRequest, IDeferrableRequest.IPrecondition> resolvedMap = 
            new OrderedMultiMap<IDeferrableRequest, IDeferrableRequest.IPrecondition> (LinkedList.class);
    
    /* This map keeps a cache of all static resolved preconditions. If this guy starts growing too much,
     * we can always delegate the task of answering whether some precondition has been met or not to a
     * more memory-conscious implementation (this would of course require designing another interface) */
    private Map<IDeferrableRequest.IPrecondition, IDeferrableRequest.IResolutionContext> metPreconditions = 
        new HashMap<IDeferrableRequest.IPrecondition, IDeferrableRequest.IResolutionContext>();
    
    public static IDeferrableRequest.IPrecondition nullPrecondition;
    public static IDeferrableRequest.IResolutionContext nullResolutionContext;
    
    static{
        StdPreconditionImpl spi = new StdPreconditionImpl();
        StdResolutionContextImpl srci = new StdResolutionContextImpl();
        spi.setType(new StdTypeImpl(IDeferrableRequest.NIL, IDeferrableRequest.MATCH_ONCE));
        srci.setPrecondition(spi);
        nullPrecondition = spi;
        nullResolutionContext = srci;
    }
	    
    private String vmid;
    
    private DeferrableRequestQueue() { 
        reset();
    }
    
    public DeferrableRequestQueue(String vmid){
        this(vmid, ALLOW_DUPLICATES);
    }
    
    public DeferrableRequestQueue(String vmid, int duplicatePolicy){
        this();
        this.vmid = vmid;
    }
    
    /** Attempts to solve an event eagerly. 
     * 
     * @param evt Request to resolve eagerly
     * @return Request reference if could resolve, null if deferred.
     */
    public boolean addEagerlyResolve(IDeferrableRequest evt) 
    	throws Exception
    {
        return addEagerlyResolve(evt, null);
    }

    /** This method was added after a few requesters started requiring access
     * to the object produced as a result of the deferrable request resolution. 
     * Though that poses no problem when the implementor of the IDeferrableRequest
     * is the same agent who makes the request, it becomes a problem if the 
     * requester is decoupled from the request class, for instance in the case 
     * of breakpoint requester needing access to the BreakpointRequest created
     * when the corresponding DeferrableBreakpointRequest gets resolved.
     * 
     * In that case, some sort of generic notification mechanism must be devised.
     * This is our solution (don't know if it's any good).
     * 
     * If you need more than one listener per request, you can implement an IResolutionListener
     * that distributes events under the hood. 
     * 
     * REMARK I had initially inserted this method in the IDeferrableRequest interface
     * but later found out that would be more generic and less painful to add it to the
     * DeferrableEventQueue. The downside is that there's no notification if the 
     * deferrable request is resolved manually.
     * 
     * REMARK This mechanism is based on the assumption that every deferred event execution
     * is taken care by this queue or by calls to this queue.
     * 
     * REMARK The only method which is meant to be called by more than one thread simultaneously
     * is resolveForContext. That's because the "environment" is responsible for calling it and
     * our hipothesis is that the "environment" could be multithreaded.
     * 
     * @param listener
     */
    public synchronized boolean addEagerlyResolve(IDeferrableRequest evt, IResolutionListener listener)
    	throws Exception
    {
        // Registers the resolution listener.
        if(listener != null)
            evt.addResolutionListener(listener);
        
        resolveEagerly(evt);
        
        // Defers if unable to.
        if(resolvedMap.size(evt) != evt.getRequirements().size()){
            addDeferrableEvent(evt);
            return false;
        }
        
        return true;
    }
    
    public synchronized boolean removeRequest(IDeferrableRequest req){
        
        boolean success = true;
        
        for(IDeferrableRequest.IPrecondition precond : req.getRequirements()){
            if(precond.getType().matchType() == STATIC)
                success &= matchOnce2request.remove(precond, req);
            else if(precond.getType().matchType() == DYNAMIC)
                success &= matchMultiple2request.remove(precond, req);
            else
                throw new UnsupportedException("Unknown precondition match type.");
        }
        
        return success;
    }

    /**
     * This method should be called from the execution environment whenever 
     * a precondition is met. The events whose list of precondition includes
     * this one will be notified and the resolution context passed onto it.
     *  
     * REMARK This method might be called by multiple threads. That's why
     * it's synchronized.
     * 
     * @param rc
     * @return
     */
    public synchronized void resolveForContext(IDeferrableRequest.IResolutionContext rc) 
    {
        /* Only static preconditions are valid */
        if(rc.getPrecondition().getType().matchType() != IDeferrableRequest.MATCH_ONCE)
            throw new UnsupportedException("Only static preconditions can be met.");
        
        if(metPreconditions.containsKey(rc.getPrecondition())){
            mh.getWarningOutput().println(
                    "Warning - duplicate precondition " + rc.getPrecondition()
                            + ".");
        }
        
        metPreconditions.put(rc.getPrecondition(), rc);
                
        /* The whole point here is to try to solve as many deferred
           event requests as we can based on some external info, namely
           the precondition. 
           We match the precondition against every possible precondition, 
           static and dynamic. */
        List<IDeferrableRequest.IPrecondition> allPreconditions = matchPreconditions(
                rc.getPrecondition(), matchOnce2request.keySet(), STATIC);
        matchPreconditions(allPreconditions, rc.getPrecondition(), matchMultiple2request.keySet(), DYNAMIC);
        
        /* Now we try to resolve the entire list - if it's not empty, of course. */
        if(allPreconditions.size() == 0) return;
        
        /* If this returns false, it means one or more event request classes are exhibiting
         * wrong behavior (this probably means buggy implementation).
         */
        if(!resolveClasses(allPreconditions, rc))
            throw new InternalError(
                    "An event whose precondition has been met has "
                    + "reported that it couldn't be resolved due to it's precondition not "
                    + "being met. This is a serious error.");
        
    }
    
    public synchronized void reset(){
        matchOnce2request.clear();
        metPreconditions.clear();
        metPreconditions.put(DeferrableRequestQueue.nullPrecondition, DeferrableRequestQueue.nullResolutionContext);
    }

    private void addDeferrableEvent(IDeferrableRequest request) {
    
        /* Adds the event to all precondition lists. */
        for (IDeferrableRequest.IPrecondition precond : request.getRequirements()) {

            /* Unless those that already have been met */
            if(resolvedMap.contains(request, precond)) continue;

            /* Preconditions are split into lists by type. */
            if(precond.getType().matchType() == IDeferrableRequest.MATCH_ONCE)
                matchOnce2request.add(precond, request);
            else if(precond.getType().matchType() == IDeferrableRequest.MATCH_MULTIPLE)
                matchMultiple2request.add(precond, request);
            else
                throw new UnsupportedException("Unknown precondition type.");
    
        }
    }

    /** This method will solve as many events from the list 'eventList' as
     * it can. Exceptions will be printed through the message handler but
     * will otherwise go unnoticed. Events that threw exceptions and could
     * not be resolved will remain on their associated classes deferred event
     * request list. 
     * 
     * 
     * @param eventList
     * @param ip
     */
    private boolean resolveClasses(
            Iterable<IDeferrableRequest.IPrecondition> requestClasses,
            IDeferrableRequest.IResolutionContext rc) 
    {
        boolean resolved = true;
        
        /* Outer loop iterates classes */
        for(IDeferrableRequest.IPrecondition requestClass : requestClasses){
            // Dangerous: assumes that if it's not static then its dynamic. Could be something else.
            Iterable<IDeferrableRequest> requests = (requestClass.getType()
                    .matchType() == IDeferrableRequest.MATCH_ONCE) ? matchOnce2request
                    .getClass(requestClass) : matchMultiple2request
                    .getClass(requestClass);
                    
            Iterator <IDeferrableRequest> it = requests.iterator();      
            /* Inner loop iterates events */
            while(it.hasNext()){
                IDeferrableRequest request = it.next();
                try{
                    Object er = request.resolveNow(rc);
                    if(er != null){
                        /* Before adding this precondition to the resolved list we check
                         * if there are any additional preconditions left to resolve.
                         */
                        if(request.getRequirements().size() == resolvedMap.size(request) + 1)
                            resolvedMap.removeClass(request); /* No longer have to keep track of this request. */
                        else
                            resolvedMap.add(request, requestClass); /* Careful with pattern/non-pattern here. */
                        it.remove();
                    }else{
                        if(requestClass.getType().matchType() == IDeferrableRequest.MATCH_ONCE)
                            resolved = false;
                    }
                }catch(Exception e){
                    MessageHandler mh = MessageHandler.getInstance();
                    mh.printStackTrace(e);
                }
            }
        }
        return resolved;
    }

    private void resolveEagerly(IDeferrableRequest evt)
    	throws Exception
    {
        for(IDeferrableRequest.IPrecondition requirement : evt.getRequirements()){

            /**
             * First acquires all resolved preconditions that could solve this requirement
             * by matching the requirement precondition agains all cached preconditions.
             */
            List<IDeferrableRequest.IPrecondition> matches = matchPreconditions(
                    requirement, metPreconditions.keySet(), STATIC);
            
            if(matches.size() == 0)
                continue;
            
            /**
             * After getting all matching (resolved) preconditions, we try to resolve the request using
             * their associated resolution context.
             * 
             * Preconditions for deferrable requests might be static or dynamic. Static preconditions map
             * to a single resolution context. For instance, having java.lang.Exception loaded as a 
             * precondition defines a static precondition. That's because once the class java.lang.Exception
             * is loaded, you either solve the request or fail. On the other hand, having the java.lang.*
             * loaded does not define a static precondition as this precondition can be achieved in a number of
             * ways. In particular, you can never know whether all classes from java.lang. are already loaded or not.
             * So, the output from IDeferrableRequest.resolveNow is interpreted diferently depending on the 
             * type of the precondition.
             * 
             * If the precondition advertised is static, the algorithm is as follows:
             * 
             *  1 - Try to resolve the request using the resolution context associated with the static
             *      precondition.
             *  
             *  2 - If it fails, then resolution fails. If it succeeds, the request is removed from the
             *      queue of requests waiting for that precondition to be fulfilled.
             *      
             * If the precondition advertised is dynamic, however, the algorithm is as follows:
             * 
             * 1 - Find all currently matching static preconditions to the dynamic precondition advertised.
             * 2 - Try to resolve the request with each precondition.
             * 3 - If resolution succeeds, remove event from queue. If it fails, it might be that the static
             *     precondition(s) that map to the dynamic precondition advertised has not yet been fulfilled.
             *     So it's not actually failing. 
             * 
             */

            IDeferrableRequest.IPreconditionType type = requirement.getType();
            
            if(type.matchType() == IDeferrableRequest.MATCH_ONCE){
            
                // MATCH_ONCE means only one static precondition should match this one.
                if(matches.size() > 1){
                    throw new InternalError("Multiple matches for static precondition.");
                }
                IDeferrableRequest.IResolutionContext ctx = metPreconditions.get(matches.get(0));
                Object ret = evt.resolveNow(ctx);
                
                // If we couldn't resolve, then resolution has failed.
                if (ret == null){
                    throw new InternalError(
                            "An event whose precondition has been met has "
                            + "reported that it couldn't be resolved due to it's precondition not "
                            + "being met. This is a serious error.");
                }
                
                // Otherwise the static precondition has been fulfilled.
                resolvedMap.add(evt, requirement);
                
            }else if(type.matchType() == IDeferrableRequest.MATCH_MULTIPLE){
                // Iterates through and tries to resolve the event request.
                Object ret = null;
                for(IDeferrableRequest.IPrecondition matched : matches){
                    IDeferrableRequest.IResolutionContext ctx = metPreconditions.get(matched);
                    ret = evt.resolveNow(ctx);
                    
                    /* Dynamic precondition has been fulfilled */
                    if(ret != null){
                        resolvedMap.add(evt, requirement);
                        break;
                    }
                }

            }else{
                throw new UnsupportedException("Unsupported precondition type " + type.matchType());
            }
        }
    }
    
    
    private List<IDeferrableRequest.IPrecondition> matchPreconditions(
            IDeferrableRequest.IPrecondition precond,
            Set<IDeferrableRequest.IPrecondition> list, byte mode_flags){
        
        List <IDeferrableRequest.IPrecondition> matches = new LinkedList <IDeferrableRequest.IPrecondition>();
        this.matchPreconditions(matches, precond, list, mode_flags);
        return matches;
    }
   
    /**
     * This method finds all preconditions contained in the set that match the precondition passed
     * as first parameter. The third parameter specifies whether just static, dynamic or both types of
     * preconditions should be matched. 
     * 
     * @param dynamic
     * @return
     */
    private void matchPreconditions(
            List <IDeferrableRequest.IPrecondition> matches,
            IDeferrableRequest.IPrecondition precond,
            Set<IDeferrableRequest.IPrecondition> list, byte mode_flags) {
        
        String static_text = null;
        
        /* Parameter precondition is static */
        if(precond.getType().matchType() == IDeferrableRequest.MATCH_ONCE){
            
            /* Match against static preconditions? */
            if(((mode_flags & STATIC) != 0) && list.contains(precond)){
                matches.add(precond);
            }
            
            static_text = precond.getClassId();
        }
        
        /* Parameter precondition is dynamic */
        else if(precond.getType().matchType() == IDeferrableRequest.MATCH_MULTIPLE){
            
            if(precond.getClassId() == null)
                throw new NestedRuntimeException(
                        new IllegalAttributeException("Multiple match preconditions must carry a pattern."));
            
            /* Match agains static preconditions? */
            if((mode_flags & STATIC) != 0){
                String pattern = precond.getClassId();
                String stem;
                boolean end;
            
                if(pattern.startsWith("*")){ 
                    end = true;
                    // Handles the degenerate case where pattern = "*"
                    stem = (pattern.length() > 1)?pattern.substring(1, pattern.length()):"";
                } else if (pattern.endsWith("*")) {
                    end = false;
                    stem = (pattern.length() > 1)?pattern.substring(0, pattern.length()-1):"";
                } else{
                    throw new NestedRuntimeException(
                            new IllegalAttributeException("Precondition patterns must either start end in a '*' ."));
                }
                
                static_text = stem;
            
                /* Now looks for all matching cached preconditions and adds them to the list. Note
                 * that this is waaay slower than lookup for static preconditions. */
                for(IDeferrableRequest.IPrecondition candidate : list){
                    String text = candidate.getClassId();
                    if(text == null) continue;
                    if(end && text.endsWith(stem)) matches.add(candidate);
                    else if(!end && text.startsWith(stem)) matches.add(candidate);
                }
            }
            
        }else 
            throw new UnsupportedException("Unknown precondition type.");
        
        /* Match against dynamic preconditions? */
        if((mode_flags & DYNAMIC) != 0){
            
            /* If the text is null, don't bother - it cannot match any dynamic preconditions. */
            if(static_text == null) return;
            
            boolean end;
            
            for(IDeferrableRequest.IPrecondition candidate : list){
                if(candidate.getType().matchType() != IDeferrableRequest.MATCH_MULTIPLE) continue;
                String pattern = candidate.getClassId();
                String stem = null;
                
                if(pattern == null){
                    // This is ugly, but I can't stand creating more exception types.
                    throw new NestedRuntimeException(
                            new IllegalAttributeException("Multiple match preconditions must carry a pattern."));
                }  
                
                /* Extracts and interprets the pattern */
                if(pattern.startsWith("*")){ 
                    end = true;
                    // Handles the degenerate case where pattern = "*"
                    stem = (pattern.length() > 1)?pattern.substring(1, pattern.length()):"";
                } else if (pattern.endsWith("*")) {
                    end = false;
                    stem = (pattern.length() > 1)?pattern.substring(0, pattern.length()-1):"";
                } else{
                    throw new NestedRuntimeException(
                            new IllegalAttributeException("Precondition patterns must either start end in a '*' ."));
                }
                
                /* Matches static precondition against pattern */
                if(end && static_text.endsWith(stem)) matches.add(candidate);
                else if(!end && static_text.startsWith(stem)) matches.add(candidate);
            }
        }

    }
}
