/*
 * Created on Sep 20, 2005
 * 
 * file: PIManagementDelegate.java
 */
package ddproto1.localagent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.omg.PortableInterceptor.Current;

import ddproto1.localagent.CORBA.orbspecific.IStampRetrievalStrategy;
import ddproto1.util.collection.ReadOnlyIterable;

public class PIManagementDelegate {
    
    private List <CurrentSpec> carriers = new LinkedList<CurrentSpec>(); 
    private ReadOnlyIterable<CurrentSpec> ROcarriers = new ReadOnlyIterable<CurrentSpec>(carriers);
    private IStampRetrievalStrategy isrs;
    private static final Logger logger = Logger.getLogger(PIManagementDelegate.class);
    
    public PIManagementDelegate(IStampRetrievalStrategy strategy){
        this.isrs = strategy;
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
    
    public String retrieve(String retrieve) throws Exception {
        if(isrs == null) 
            throw new IllegalStateException("No stamp retrieval strategy set for PIManagementDelegate.");
        
        /** Instrumentation hook has been called but there are no registered interceptors. */
        if(carriers.isEmpty()){
            logger.warn("Server-side debug interceptors have not been properly initialized for this node. " +
                    "However, at least part of the code has been instrumented. If you intend to use the " +
                    "debugger, this is an error. Please make shure the correct ORB initializer has been installed.");
            return null;
        }
        
        return isrs.retrieve(retrieve, carriers.iterator());
    }
    
    public Iterable<CurrentSpec>getAllPICurrents(){
        return ROcarriers;
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
