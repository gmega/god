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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;

import ddproto1.commons.DebuggerConstants;
import ddproto1.commons.Event;
import ddproto1.exception.CommException;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.CORBA.orbspecific.IStampRetrievalStrategy;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;
import ddproto1.util.traits.ConversionTrait;

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
    
    private static final ConversionTrait fh = ConversionTrait.getInstance();
    
    private Logger logger = Logger.getLogger(ORBHolder.class);
    private IStampRetrievalStrategy isrs;
    private IGlobalAgent global; 
    private ThreadLocal <Integer> endPoint = new ThreadLocal <Integer>();
    
    public synchronized static ORBHolder getInstance()
    	throws IOException, UnknownHostException 
    {
        return (instance == null)?(instance = new ORBHolder()):(instance);
    }
    
    private List <CurrentSpec> carriers = new LinkedList<CurrentSpec>(); 
    
    private ORBHolder()
    	throws IOException, UnknownHostException
    {
        if(global == null)
            global = GlobalAgentFactory.getInstance().resolveReference();
    }
    
    /**
     * Maintains an internal registry of all operational PICurrent objects
     * in the application.
     * 
     * @param c - A reference to a PICurrent object
     * @param partSlot - A slot allocated for propagating causal information
     * @param idSlot - A slot allocated for telling the client-side interceptor 
     * which local thread is currently making the call.
     * @param opSlot - A slot allocated for telling the the client-side interceptor
     * the name of the currently invoked operation. 
     * @param remSlot - Used by local agent code to differ between local and remote
     * calls.
     * @param topSlot - Slot used to convey the size of the call stack at the moment
     * the stub was invoked. This info will be later on transmitted to the global 
     * agent by the client-side debug interceptor.
     */
    public void registerPICurrent(Current c, int partSlot, int idSlot, int opSlot,
            int remSlot, int topSlot, int stepSlot){
        CurrentSpec cs = new CurrentSpec(c, partSlot, idSlot, opSlot, remSlot, topSlot, stepSlot);
        carriers.add(cs);
    }
    /**
     * Unregisters a previously registered <i>PICurrent</i> object. 
     * 
     * @param c
     */
    public void unregisterPICurrent(Current c){
        Iterator it = carriers.iterator();
        while(it.hasNext()){
            CurrentSpec cs = (CurrentSpec)it.next();
            if(cs.c.equals(c))
                carriers.remove(cs);
        }
    }
    
    /**
     * Sets an orb-specific <code>IStampRetrievalStrategy</code> for this
     * ORBHolder. The practical implication of this is that the client may
     * not mix up ORB implementations.  
     * 
     * @param isrs
     */
    public void setStampRetrievalStrategy(IStampRetrievalStrategy isrs){
        this.isrs = isrs;
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
        
        logger.debug("Now retrieving stamp (server-side)");
        
        if(isrs == null){
            logger.fatal("Cannot retrieve ID - no retrieval " +
            		" strategy has been configured. Debugger is not working" +
            		" properly.");
            return;
        }
        
        Tagger t = Tagger.getInstance();
        try{
            /* First checks if this is a remote or a local call */
            String isRemote = isrs.retrieve("locality", carriers.iterator(), t);
            /* Call is local - nothing to do. */
            if(isRemote.equals("false")) return;
            
            /* Remove remote marker, since this call might chain up with other
             * instrumented servants and if they think it's a remote call they
             * might misintepret data and/or send erroneous or duplicate information 
             * to the central agent.
             */
            for(CurrentSpec cs : carriers){
                Any any = cs.c.get_slot(cs.getREMSlot());
                
                /* I *think* this will never be null (since if it were null then
                 * the retrieval strategy should have returned false in the first
                 * place).
                 */
                assert(any != null);
                
                any.insert_boolean(false);
                cs.c.set_slot(cs.getREMSlot(), any);
            }
                
                        
            /* Obtains the distributed thread id from the PICurrent object
             * through an orb-specific strategy.
             */
            String tag = isrs.retrieve("stamp", carriers.iterator(), t);
            String op = isrs.retrieve("opname", carriers.iterator(), t);
            
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
            logger.debug("Calling operation " + fullOp + " at stack frame " + stackTrace.length);
            
            /* Notifies the central agent that he must insert a breakpoint
             * right away.
             */
            /*REMARK Creating hashmaps might be expensive but it is less expensive
             than having to synchronize thread access to a global hashmap */
            
            Map <String, String> infoMap = new HashMap<String, String>();
            
            String ltid = fh.int2Hex(Tagger.getInstance().currentTag());
            
            /* Current distributed thread id */
            infoMap.put("dtid", tag);	
            /* Local thread id mapped to distributed thread at the servant level */
            infoMap.put("ltid", ltid);
            /* Size of the call stack when remote call begun (without counting our stack frame)*/
            infoMap.put("siz", String.valueOf(stackTrace.length - 1));
            /* We must also make this information available locally */
            endPoint.set(new Integer(stackTrace.length));

            /* Name of the called operation */
            infoMap.put("op", op);
            /* Full name of the called operation */
            infoMap.put("fop", fullOp);

            /* Build a message with all that information */
            Event e = new Event(infoMap, DebuggerConstants.SERVER_RECEIVE);
            
            try{
                logger.debug("Notifying the central agent of SERVER_RECEIVE.");
                /* Pump it up to the global agent */
                global.syncNotify(e);
            }catch(CommException ce){
                logger.fatal("Failed to notify the central agent of a server receive event - " +
                		" central agent's tracking state might be inconsistent.", ce);
            }
            
        }catch(Exception e){
            logger.fatal("Cannot retrieve ID - retrieval strategy error.", e);
        }
    }
    
    /**
     * Method called from instrumented servant code right before the servant method ends.
     */
    public void checkOut(){
        /* Introspects the stack */
        Throwable t = new Throwable();
        StackTraceElement [] callStack = t.getStackTrace();
        
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
         */
        //if(callStack.length <= base){
        assert(callStack.length >= 2);
        if((callStack.length - 1) <= base){
            Tagger tagger = Tagger.getInstance();
            String ltid = fh.int2Hex(tagger.currentTag());
            String dtid = fh.int2Hex(tagger.partOf().intValue());
            
            //StackTraceElement servantCall = callStack[0];
            StackTraceElement servantCall = callStack[1];
            
            /* This is for consistency */
            String fullOp = servantCall.getClassName() + "." + servantCall.getMethodName() + "()";
            
            /* Builds map information */
            Map <String, String> infoMap = new HashMap<String, String>();
            infoMap.put("fop", fullOp);
            infoMap.put("siz", String.valueOf(callStack.length));
            /* REMARK This is actually the only required information. Everything 
             * else is for consistency.
             * TODO Determine if this is all really required.
             */
            infoMap.put("dtid", dtid); 
            infoMap.put("ltid", ltid);
            
            /* Build a message with all that information */
            Event e = new Event(infoMap, DebuggerConstants.SIGNAL_BOUNDARY);
            
            int stepStats;
            
            try{
                /* Pump it up to the global agent */
                stepStats = global.syncNotify(e);
            }catch(CommException ce){
                logger.fatal("Failed to notify the central agent of a client upcall - " +
                		" central agent's tracking state might be inconsistent.", ce);
                return;
            }
            
            if(stepStats == DebuggerConstants.STEPPING){
                Any any = ORB.init().create_any();
                any.insert_boolean(true);
                
                for(CurrentSpec cs : carriers){
                    try{
                        cs.getCurrent().set_slot(cs.getSTPSlot(), any);
                    }catch(InvalidSlot ex){
                        logger.error("Cannot insert info into slot.", ex);
                    }
                }
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
        /* First things first - the thread might not have been marked yet */
        Tagger tagger = Tagger.getInstance();
        try{
            tagger.currentTag();
        }catch(IllegalStateException e){
            tagger.tagCurrent();
        }
            
        Iterator it = carriers.iterator();

        if(logger.isDebugEnabled())
            logger.debug("ThreadLocal gid " + tagger.currentTag() + " -> all PICurrent ");

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

        while(it.hasNext()){
            if (logger.isDebugEnabled()) {
                logger.debug("Thread id: " + tagger.currentTag() + ", part of:"
                        + tagger.partOf().intValue()
                        + " - inserted into PICurrent.");
            }

            CurrentSpec cs = (CurrentSpec)it.next();
            try{
                cs.c.set_slot(cs.dtslot, dt_any);
                cs.c.set_slot(cs.ltslot, lt_any);
                cs.c.set_slot(cs.tpslot, tp_any);
            }catch(InvalidSlot e){
                logger.fatal(e.toString(), e);
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
    
    /** Utility class that holds data about the potentially various PICurrent objects
     * created at startup of each <b>ORB</b>.
     * 
     * @author giuliano
     *
     */
    public class CurrentSpec{
        protected Current c;
        protected int dtslot;
        protected int ltslot;
        protected int opslot;
        protected int remslot;
        protected int tpslot;
        protected int stslot;
        
        public CurrentSpec(Current c, int dtslot, int ltslot, int opslot, int remslot, int tpslot, int stslot){
            this.c = c;
            this.dtslot = dtslot;
            this.ltslot = ltslot;
            this.opslot = opslot;
            this.remslot = remslot;
            this.tpslot = tpslot;
        }
        
        public int getDTSlot() { return dtslot; }
        public int getOPSlot() { return opslot; }
        public int getIDSlot() { return ltslot; }
        public int getREMSlot() { return remslot; }
        public int getTOPSlot() { return tpslot; }
        public int getSTPSlot() {return stslot; }
        public Current getCurrent() { return c; }
    }
}
