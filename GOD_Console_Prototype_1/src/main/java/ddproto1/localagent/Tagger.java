/*
 * Created on Sep 1, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Tagger.java
 */

package ddproto1.localagent;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import ddproto1.util.traits.commons.ConversionUtil;

/**
 * The tagger class is responsible for managing local VM tags for running
 * threads. Each node in the distributed system has its own tagger class. 
 * This class should be loaded by the boot classloader, or at a classloader
 * that guarantees that all classloaders of this application will see the 
 * same instance of the Tagger class. <BR> 
 * <BR>
 * 
 * <b>First version</b> - Tagging was performed automatically by a code snippet
 * inserted at the beginning of each run method. The code snippet called tagCurrent
 * as soon as the thread began execution. <BR>
 * <BR>
 * 
 * <b>Second version</b> - Due to extensive problems with the first approach (it required
 * a fairly complicated mechanism to tell the central agent which ThreadReferences corresponded
 * to each assigned id's), we've decided to let the central agent call tagCurrent remotely 
 * through the debug interface when it receives the <b>ThreadStartEvent</b>. It might slow
 * down thread creation but it's a lot cleaner and less error-prone. <BR>
 * <BR>
 * 
 * <b> Third version</b> Realized the second approach is unworkable. That means we'll have 
 * to implement the "fairly complicated" mechanism mentioned earlier. Responsibility for 
 * tagging will be moved back to the Tagger class. <BR>
 * 
 * 
 * @author giuliano
 *
 */
public class Tagger{
    
    /** Singleton instance */
    private static final Tagger instance = new Tagger();
    
    /** 24 bits is the size of the thread counter. **/
    private static final int MAX_THREAD_ID = (1 << 24) - 1;
    
    /** Marker object used to taint threads that are stepping. */
    private static final Object STEP_MARKER = new Object();

    /** Loggers for debugging and error reporting. */
    private static final Logger getSetLogger = Logger.getLogger(Tagger.class.getName() + ".getSetLogger");
    private static final Logger stepPartLogger = Logger.getLogger(Tagger.class.getName() + ".stepPartLogger");
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    /** Local thread ID generator */
    private final AtomicInteger fCounter = new AtomicInteger();

    /** Unique ID of this node. */
    private volatile Byte fNodeId;
    
    /** Unique ID as a mask to be applied to the local IDs 
     * to create the globally unique ID for each thread. 
     */
    private volatile int fGidMask = 0;
    
    /** Thread local that carries step markers for each application 
     * thread. */
    private final ThreadLocal<Object>  fStepping   = new ThreadLocal<Object>();
    
    /** Thread local that contains the registered global ID of 
     * each application thread.
     */
    private final ThreadLocal<Integer> fThreadGids = new ThreadLocal<Integer> ();
    
    /** Thread local that contains a local copy of the mapping between
     * local threads and distributed threads.
     */
    private final ThreadLocal<Integer> fPartOfMap  = new ThreadLocal<Integer> ();
    
    /** Private constructor - Tagger is a singleton */
    private Tagger() { }
    
    /**
     * Gives access to the Tagger singleton.
     * 
     * @return The singleton instance.
     */
    public static Tagger getInstance(){
        return instance;
    }
    
    /**
     * Sets the Global ID for the currently running process. This
     * method should be called only once.
     * 
     * @param gid the global ID
     * @throws IllegalStateException if the GID has already been set. 
     */
    public synchronized void setGID(byte gid){
        if(fNodeId != null) 
            throw new IllegalStateException("Cannot set gid twice.");
        fGidMask = (gid << 24);
        fNodeId  = gid;
    }
    
    /**
     * Gets the Global ID of the currently running process.
     * 
     * @return the GID for this process.
     * @throws IllegalStateException if the GID hasn't been set.
     */
    public byte getGID(){
        checkGidSet();
        return fNodeId;
    }

    /**
     * Tags the current thread with a globally unique ID.
     * 
     * @throws IllegalStateException if the GID for the currently running
     * process hasn't yet been set with setGID(byte).
     */
    public void tagCurrent(){
        checkGidSet();
        Integer ltuid = (Integer)fThreadGids.get();
        if(ltuid != null) return;
        
        ltuid = new Integer(this.nextUID() | fGidMask);
        fThreadGids.set(ltuid);
        if(getSetLogger.isDebugEnabled())
            getSetLogger.debug("Tagged thread at process " + 
                    this.getGID() 
                    + " with tag " 
                    + ct.uuid2Dotted(ltuid.intValue()));
        
        this.haltForRegistration();
    }
    
    /**
     * Returns the next locally unique ID.  
     * @return
     */
    protected synchronized int nextUID(){
        int nxt = fCounter.incrementAndGet();
        if(nxt > MAX_THREAD_ID) 
            throw new IllegalStateException("Thread counter has overflowed." +
                    " Debugger can't operate correctly.");
        return nxt;
    }
    
    /** 
     * Special method called by instrumentation code. This method has a 
     * breakpoint placed in it that will trigger a JPDA event.
     */
    private void haltForRegistration(){ }
    
    /**
     * Stpecial method called by instrumentation code. This method is called
     * by stepping threads to request them to be positioned into application
     * code.
     *
     */
    public void stepMeOut() {
    	if(stepPartLogger.isDebugEnabled()){
    		Integer lt_uuid = this.currentTag();
    		String id = (lt_uuid == null)?"<unregistered>":ct.uuid2Dotted(lt_uuid);
    		stepPartLogger.debug("Thread " + id + " entering stepping mode protocol.");
    	}
        this.unsetStepping(); // Clears the step status (Do I really have to?)
    }
    
    /**
     * Dettaches the current thread from its global ID.
     */
    public void untagCurrent(){
        fThreadGids.set(null);
    }
    
    /**
     * Anwers whether the current thread has been assigned a global ID
     * or not.
     * 
     * @return
     */
    public boolean isCurrentTagged(){
        return (fThreadGids.get() != null);
    }
    
    /**
     * Returns the globally unique ID for the current thread.
     * 
     * @return the globally unique ID for this thread.
     * @throws java.util.NoSuchElementException if no ID has yet been
     * assigned to this thread.
     */
    public int currentTag(){
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Retrieving tag.");
        
        Integer tgid = (Integer)fThreadGids.get();
        if(tgid == null)
            throw new NoSuchElementException("Cannot retrieve a thread marker before it gets set");
        
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Retrieved tag " + tgid
                    + " that corresponds to " + ct.uuid2Dotted(tgid.intValue()));
        
        return tgid.intValue();
    }
    
    /** 
     * Tells if the current thread is stepping or not.
     * 
     * @param uuid
     * @return
     */
    public boolean isStepping(){
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Tagger.isStepping() called.");
        return fStepping.get() != null;
    }
    
    /**
     * Adds a step marker to the current thread.
     */
    public void setStepping(){
        fStepping.set(STEP_MARKER);
    }
    
    /**
     * Unsets the step marker from the current thread.
     */
    public void unsetStepping(){
        fStepping.remove();
    }
    
    /** Makes the caller local thread part of the distributed thread
     * with ID 'uuid'.
     * 
     * @param uuid
     */
    public void makePartOf(int uuid){
        if(stepPartLogger.isDebugEnabled()){
            int id = this.currentTag();
            stepPartLogger.debug("Will BIND local thread " + ct.uuid2Dotted(id)
                    + " to distributed thread " + ct.uuid2Dotted(uuid));
        }
        Integer current = (Integer)fPartOfMap.get();
        if(current != null){
            stepPartLogger
                    .error("Local thread "
                            + ct.uuid2Dotted(currentTag()) + " is being marked twice as part of "
                            + ((current.intValue() == uuid) ? "the same distributed thread (NOT THAT BAD)"
                                    : "different distributed threads (THIS IS REALLY BAD)") + ".");
        }
        fPartOfMap.set(new Integer(uuid));
    }
    
    /**
     * Returns the ID of the distributed thread that currently encloses this
     * local thread.
     * 
     * @return
     */
    public Integer getEnclosingDT(){
    	return fPartOfMap.get();
    }
    
    /**
     * Unbinds the current thread from the distributed thread that currently 
     * encloses it.
     * 
     * @param uuid
     */
    public void unmakePartOf(){
        Integer current = (Integer)fPartOfMap.get();
        if(current == null){
            stepPartLogger
                    .error("Local thread "
                            + ct.uuid2Dotted(currentTag()) + " is being unmarked twice as part of a"
                            + "distributed thread.");
            
            return;
        }
        
        if(stepPartLogger.isDebugEnabled()){
            int id = this.currentTag();
            stepPartLogger.debug("Will UNBIND local thread " + ct.uuid2Dotted(id)
                    + " from distributed thread " + ct.uuid2Dotted(current));
        }

        fPartOfMap.remove();
    }
    
    /**
     * Futurely, this will retrieve the data from the request interceptor context
     * and insert it into thread-local storage.
     */
    public void retrieveStamp(){
        // NO-OP, we're doing this at the interceptor. I must change that. 
    }
    
    /**
     * Returns the ID 
     * @return
     */
    public Integer partOf(){
        Integer partUUID = (Integer)fPartOfMap.get();
        Integer myUUID = (Integer)fThreadGids.get();
        assert(partUUID != null || myUUID != null);
        return (partUUID == null)?myUUID:partUUID;
    }
    
    private void checkGidSet(){
        if(fNodeId == null)
            throw new IllegalStateException("GID must be set before " +
                    "attempting to call this operation.");
    }

}
