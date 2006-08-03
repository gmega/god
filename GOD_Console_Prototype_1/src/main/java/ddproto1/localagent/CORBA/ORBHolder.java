/*
 * Created on Sep 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ORBHolder.java
 */

package ddproto1.localagent.CORBA;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.exception.commons.CommException;
import ddproto1.localagent.PIManagementDelegate;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.PIManagementDelegate.CurrentSpec;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;
import ddproto1.util.commons.Event;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * This class holds most of the interception code that is called by the various 
 * code snippets inserted both at stubs and servants. It is a <b>singleton</b>.
 * 
 * This tactic is used to simplify bytecode instrumentation. We don't know if it
 * could cause undesired behavior (believe not).
 * 
 * @author giuliano
 *
 */
public class ORBHolder {
    
    private static ORBHolder instance = null;
    
    private static final ConversionUtil fh = ConversionUtil.getInstance();
    
    private Logger inLogger = Logger.getLogger(ORBHolder.class.getName() + ".inLogger");
    private Logger notificationLogger = Logger.getLogger(ORBHolder.class.getName() + ".notificationLogger");
    private Logger insertLogger = Logger.getLogger(ORBHolder.class.getName() + ".insertLogger");
    
    private PIManagementDelegate delegate;
    
    private IGlobalAgent global; 
    private ThreadLocal <Integer> endPoint = new ThreadLocal <Integer>();
    
    public synchronized static ORBHolder getInstance()
    	throws IOException, UnknownHostException 
    {
        return (instance == null)?(instance = new ORBHolder()):(instance);
    }
    
    private ORBHolder()
    	throws IOException, UnknownHostException
    {
        if(global == null)
            global = GlobalAgentFactory.getInstance().resolveReference();
    }
    
    /**
     * Method called from the code snippet inserted at the beginning of each
     * servant. The point just before the servant begins executing application
     * code is the only point where we can determine accurately:
     * 
     * <ol>
     * <li><b>1</b> - Operation name (obtainable from <i>PICurrent</i>), </li>
     * <li><b>2</b> - distributed thread id (obtainable from <i>PICurrent</i>),</li>
     * <li><b>3</b> - local thread code, </li>
     * <li><b>4</b> - servant class (obtainable by introspecting the call stack). </li> 
     * </ol>      
     */
    public void retrieveStamp(){
        
        if(!checkState()) return;
        
        if(inLogger.isDebugEnabled()){
            Throwable t = new Throwable();
            StackTraceElement [] callStack = t.getStackTrace();
            inLogger.debug("retrieveStamp() called by " + callStack[1].getMethodName() + "() at the server-side.");
        }
        
        Tagger t = Tagger.getInstance();
        try{
            /* First checks if this is a remote or a local call */
            String isRemote = delegate.retrieve("locality");
            /* Call is local - nothing to do. */
            if(isRemote.equals("false")){
                if(inLogger.isDebugEnabled())
                    inLogger.debug("Call was not remote.");
                return;
            }
            
            /* Remove remote marker, since this call might chain up with other
             * instrumented servants and if they think it's a remote call they
             * might misintepret data and/or send erroneous or duplicate information 
             * to the central agent.
             */
            for(CurrentSpec cs : delegate.getAllPICurrents()){
                Current picurrent = cs.getCurrent();
                Any any = cs.getCurrent().get_slot(cs.getREMSlot());
                                
                /* I *think* this will never be null (since if it were null then
                 * the retrieval strategy should have returned false in the first
                 * place).
                 */
                assert(any != null);
                
                any.insert_boolean(false);
                picurrent.set_slot(cs.getREMSlot(), any);
            }
                
                        
            /* Obtains the distributed thread id from the PICurrent object
             * through an orb-specific strategy.
             */
            String tag = delegate.retrieve("stamp");
            String op = delegate.retrieve("opname");
            
            /* Mark the current thread if it hasn't been marked. */
            Tagger.getInstance().tagCurrent();
                                    
            /* Makes the current thread a part of the distributed thread
             * that made the remote call.
             */
            if(tag != null)
                t.makePartOf((int)fh.hex2Int(tag));
            
            /* Introspects the call stack to find out the class of the 
             * enclosing servant (needed to set the breakpoint). Also finds out 
             * the position of the remote call on the call stack (needed so 
             * we can determine when to stop showing running code to the user).
             */
            Throwable ex = new Throwable();
            StackTraceElement [] stackTrace = ex.getStackTrace();
            
            /* Assertion: At least the currently executing method 
             * and the servant method must be in the call stack. */
            assert (stackTrace.length >= 2);
            StackTraceElement servantCall = stackTrace[1];
            String fullOp = servantCall.getClassName() + "." + servantCall.getMethodName();

            /* Notifies the central agent that he must insert a breakpoint
             * right away.
             */
            /*REMARK Creating hashmaps might be expensive but it is less expensive
             than having to synchronize thread access to a global hashmap */
            
            Map <String, String> infoMap = new HashMap<String, String>();
            
            int _ltid = Tagger.getInstance().currentTag();
            String ltid = fh.int2Hex(_ltid);
            
            /* Current distributed thread id */
            infoMap.put("dtid", tag);	
            /* Local thread id mapped to distributed thread at the servant level */
            infoMap.put("ltid", ltid);
            /* Size of the call stack when remote call begun (without counting our stack frame)*/
            infoMap.put("siz", String.valueOf(stackTrace.length - 1));
            /* We must also make this information available locally */
            endPoint.set(new Integer(stackTrace.length));

            /* CORBA name of the called operation */
            infoMap.put("op", op);
            /* Java name of the called operation */
            infoMap.put("fop", fullOp);
            
            /* Line number of the running code, inserted by the intrumentation hook. */
            infoMap.put("lin", Integer.toString(servantCall.getLineNumber()+1));
            /* Class in which the operation is being invoked. */
            infoMap.put("cls", servantCall.getClassName());
            

            /* Build a message with all that information */
            Event e = new Event(infoMap, DebuggerConstants.SERVER_RECEIVE);
            
            try{
                if(notificationLogger.isDebugEnabled()){
                    notificationLogger.debug("Notifying global agent of SERVER_RECEIVE:" +
                                 "\n  Server-side operation: " + fullOp + 
                                 "\n  Local thread stack frame base: " + (stackTrace.length -1)+
                                 "\n  Distributed thread ID: " + fh.uuid2Dotted(fh.hex2Int(tag)) +  
                                 "\n  Local thread: " + fh.uuid2Dotted(_ltid));
                }
                /* Pump it up to the global agent */
                global.syncNotify(e);
            }catch(CommException ce){
                notificationLogger.fatal("Failed to notify the central agent of a server receive event - " +
                		" central agent's tracking state might be inconsistent.", ce);
            }
            
        }catch(Exception e){
            notificationLogger.fatal("Cannot retrieve ID - retrieval strategy error.", e);
        }
    }
    
    /**
     * Method called from instrumented servant code right before the servant method ends.
     */
    public void checkOut(){
        if(!checkState()) return;
        
        /* Introspects the stack */
        Throwable t = new Throwable();
        StackTraceElement [] callStack = t.getStackTrace();

        /* There has to be at least three frames.
         * 0 - checkOut()
         * 1 - finally_block
         * 2 - application_method.
         */
        assert callStack.length >= 3;
        
        String method = callStack[2].getMethodName();
        
        if(inLogger.isDebugEnabled()){
            inLogger.debug("checkOut() called at instrumented servant code for method " + method + "()"); 
        }

        
        Integer _base = (Integer)endPoint.get();
        /* Probably not a remote call. */
        if(_base == null){
            /* Makes an assertion */
            try{
                int ctag = Tagger.getInstance().currentTag();
                int ptag = Tagger.getInstance().partOf().intValue();
                /* If ctag != ptag it means this thread is a local
                 * representative of a distributed thread. This also 
                 * means this local representative should have a base frame
                 * assigned (I think).
                 */
                assert(ctag == ptag);
            }catch(IllegalStateException e){
                /* Ok, it's definitely NOT a remote call. */
                inLogger.debug("Not a remote call for method " + method + "()");
            }
            return;
        }
        /* Obtains the stack size of the initial call */
        int base = ((Integer)endPoint.get()).intValue();
        
        /* Transpassed the base. Notify the local agent of
         * return.
         * 
         * We have to decrement callStack.length by 1 before comparing
         * because we're assuming checkOut to be called from inside the "finally
         * hook" inserted by the instrumentation code. This is something specific 
         * to the instrumentation method currently in use and REMARK WILL BREAK if 
         * the instrumentation method changes. 
         * 
         * Note that it's minus 1 and not minus 2 because the initial call stack
         * length is computed from inside 'retrieveStamp'. 
         */
        //if(callStack.length <= base){
        if((callStack.length - 1) <= base){
            Tagger tagger = Tagger.getInstance();
            int _ltid = tagger.currentTag();
            String ltid = fh.int2Hex(_ltid);
            int _dtid = tagger.partOf().intValue();
            String dtid = fh.int2Hex(_dtid);
            
            //StackTraceElement servantCall = callStack[0];
            StackTraceElement servantCall = callStack[2];
            
            /* This is for consistency */
            String fullOp = servantCall.getClassName() + "." + servantCall.getMethodName();
            
            /* Builds map information */
            Map <String, String> infoMap = new HashMap<String, String>();
            /* REMARK The distributed thread id (dtid) is actually the only required information. 
             * Everything else is for consistency checks at the distributed tracker.
             * TODO Determine if this is all really required.
             */
            infoMap.put("fop", fullOp);
            infoMap.put("siz", String.valueOf(callStack.length-2));
            infoMap.put("dtid", dtid); 
            infoMap.put("ltid", ltid);
            
            /* Build a message with all that information */
            Event e = new Event(infoMap, DebuggerConstants.SIGNAL_BOUNDARY);
            
            int stepStats;
            
            /* Unbinds the current thread from the current distributed thread.*/
            tagger.unmakePartOf(_dtid);
            
            /* This seems innocuous, but it's important to avoid catastrophic
             * behavior when for some reason two calls are dispatched to the
             * servant instead of one. This happens with some special ORB objects
             * (notoriously the NameContextExtImpl) and may lead to two SIGNAL_BOUNDARIES
             * causing an assertion failure at the global agent. 
             * 
             * The hypothesis and expected behavior are cleared out by this:
             * 
             * 1) We assume the POA will dispatch a single call to only one instrumented
             *    servant method per CORBA request.
             * 
             * 2) If by any reason the POA dispatches two calls (as in the case of NameContextExtImpl),
             *    we treat the second call as non-remote and ORB related. This is ensured by setting
             *    endPoint to null for this thread.
             *    
             * Note that (2) is exceptional behavior, its NOT correct behavior. But it's well-defined. 
             * The user can always correct the erroneous behavior by excluding the non-CORBA related
             * methods from instrumentation.
             *    
             */
            endPoint.set(null);
            
            StringBuffer debugBuffer = null;
            
            try{
                if(notificationLogger.isDebugEnabled()){
                    debugBuffer = new StringBuffer();
                    debugBuffer.append("\n  Server-side operation: " + fullOp + 
                            "\n  Local thread stack frame base: " + callStack.length +
                            "\n  Distributed thread ID: " + fh.uuid2Dotted(_dtid) +  
                            "\n  Local thread: " + fh.uuid2Dotted(_ltid));
                }

                /* Pump it up to the global agent */
                stepStats = global.syncNotify(e);
            }catch(CommException ce){
                notificationLogger.fatal("Failed to notify the central agent of a client upcall - " +
                		" central agent's tracking state might be inconsistent. " + debugBuffer.toString(), ce);
                return;
            }
            
            if(notificationLogger.isDebugEnabled()){
                notificationLogger.debug("Notified global agent of SIGNAL_BOUNDARY." + 
                                            debugBuffer.toString() + 
                                            "\n Thread status: " + fh.statusText(stepStats));
            }

            /** If I plan to support disabling of remote mode, I should probably relax these restrictions. */
            if((stepStats & DebuggerConstants.ILLUSION) != 0 || stepStats == DebuggerConstants.RUNNING){
                Any any = ORB.init().create_any();
                any.insert_short((short)stepStats);
                
                for(CurrentSpec cs : delegate.getAllPICurrents()){
                    try{
                        if(notificationLogger.isDebugEnabled())
                            insertLogger.debug(" Inserting short: " + stepStats + 
                                             "\n Into slot: " + cs.getSTPSlot() + 
                                             "\n of PICurrent for operation: " + fullOp + 
                                             "\n Status inserted was: " + fh.statusText(stepStats) + 
                                             "\n Distributed thread ID: " + fh.uuid2Dotted(_dtid));
                        cs.getCurrent().set_slot(cs.getSTPSlot(), any);
                    }catch(InvalidSlot ex){
                        notificationLogger.error("Cannot insert info into slot. Step mode might not operate correctly.", ex);
                    }
                }
            }else{
                notificationLogger.error("Threads that are doing requests must be either running or stepping.");
            }
        }
    }
    
    /**
     * Method called from instrumented stub code that transfers the local thread
     * <b>UUID</b> (or <b>U</b>niversally <b>U</b>nique <b>ID</b>entifier) from 
     * thread-specific storage to the <b>PICurrent</b> object. It also propagates
     * any <i>distributed thread</i> context information that might be present in case
     * the local thread is part of another <i>distributed thread</i>.
     * 
     *
     */
    public void setStamp(){
        
        if(!checkState()) return;
        
        if(inLogger.isDebugEnabled()){
            inLogger.debug("setStamp() called at the client-side.");
        }
        /* First things first - the thread might not have been marked yet */
        Tagger tagger = Tagger.getInstance();
        try{
            tagger.currentTag();
        }catch(IllegalStateException e){
            tagger.tagCurrent();
        }
            
        if(inLogger.isDebugEnabled())
            inLogger.debug("Data from thread gid " + tagger.currentTag() + " copied to PICurrent ");

        /* REMARK I assume here that no ORB modifies the ANY object. If that does not
         * stand correct, then we must create an ANY instance for each PICurrent object 
         * (potentially making things even slower then they already are). 
         */
        Any dt_any = ORB.init().create_any();
        Any lt_any = ORB.init().create_any();
        Any tp_any = ORB.init().create_any();
        
        /* Introspects the stack to know it's size */
        Throwable t = new Throwable();
        int stack_size = t.getStackTrace().length - 1; // Doesn't count our frame

        /* lt_any's value might be equal to dt_any's but it's not always true 
         * (that's why we pass'em both) */
        lt_any.insert_long(tagger.currentTag());
        dt_any.insert_long(tagger.partOf().intValue());
        tp_any.insert_long(stack_size);

        for(CurrentSpec cs : delegate.getAllPICurrents()){
            if (inLogger.isDebugEnabled()) {
                inLogger.debug("Thread id: " + fh.uuid2Dotted(tagger.currentTag()) + ", part of:"
                        + fh.uuid2Dotted(tagger.partOf().intValue())
                        + " - inserted into PICurrent.");
            }

            try{
                Current current = cs.getCurrent();
                current.set_slot(cs.getDTSlot(), dt_any);
                current.set_slot(cs.getIDSlot(), lt_any);
                current.set_slot(cs.getTOPSlot(), tp_any);
            }catch(InvalidSlot e){
                inLogger.fatal(e.toString(), e);
            }
        }
        
        /* Unmarks the current thread. */
        //tagger.untagCurrent();
        /* 
         * Comment on the above commented line:
         * 
         * What the heck was I thinking when I did this?? I mean, 
         * this line literally blows things up. Imagine what happens
         * if this thread makes another remote call? It will have lost
         * it's ID and therefore will be ignored by the debugger. Anyway,
         * my server-side assertions detected the bug, and that's good.
         * 
         * 
         * I think this line was there because I had this weird thought
         * that an ORB could actually reuse application-side threads 
         * somehow. I hope this never happens, as it would be a major 
         * blow to this piece of software as a whole. On second thought, 
         * this could work if we reapplied the ID to the thread when it 
         * were about to return from the stub call. Anyway, I'll only
         * bother to implement this if someone tells me of an ORB that
         * reuses application-side threads (which is a bit tough to do, 
         * since you have to reconstruct a lot of stuff on return, including
         * the original call stack - and your're still not free of someone
         * using a thread local variable and getting wrong results).
         * 
         */ 

    }
    
    public void setPICManagementDelegate(PIManagementDelegate delegate){
        this.delegate = delegate;
    }
    
    public PIManagementDelegate getPICManagementDelegate(){
        this.checkState();
        return delegate;
    }
    
    private boolean checkState(){
        if(delegate == null){
            inLogger.fatal("Cannot retrieve ID - no Portable Interceptor Management " +
                    " delegate configured. Debugger is not working properly.");
            return false;
        }
        
        return true;
    }
}
