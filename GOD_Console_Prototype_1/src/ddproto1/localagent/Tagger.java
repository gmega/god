/*
 * Created on Sep 1, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Tagger.java
 */

package ddproto1.localagent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;

/**
 * The tagger class is responsible for managing local VM tags for running
 * threads. Each node in the distributed system has its own tagger class. <BR> 
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
public class Tagger extends TagResponsible{
    
    private static Tagger instance;
    private static Logger getSetLogger = Logger.getLogger(Tagger.class.getName() + ".getSetLogger");
    private static Logger stepPartLogger = Logger.getLogger(Tagger.class.getName() + ".stepPartLogger");
   
    private boolean gidSet = false;
    
    private byte gid;
    
    private Map <Integer, Byte> stepping = new HashMap<Integer, Byte>();
    
    private ThreadLocal <Integer> currentguid = new ThreadLocal <Integer> ();
    private ThreadLocal <Integer> partOf      = new ThreadLocal <Integer> ();
    
    private PIManagementDelegate delegate;
    
    private Tagger() {}
    
    public synchronized static Tagger getInstance(){
        return (instance == null)?(instance = new Tagger()):instance;
    }
    
    public void setGID(byte gid){
        this.gid = gid;
        gidSet = true;
    }
    
    public void setPICManagementDelegate(PIManagementDelegate delegate){
        this.delegate = delegate;
    }
    
    public byte getGID(){
        if(!gidSet) 
            throw new IllegalStateException("GID must be set before attempting to mark any threads");
        return gid;
    }

    public void tagCurrent(){
        Integer ltuid = (Integer)currentguid.get();
        if(ltuid != null) return;
        
        ltuid = new Integer(this.nextUID());
        currentguid.set(ltuid);
        if(getSetLogger.isDebugEnabled())
            getSetLogger.debug("Tagged thread at process " + this.getGID() + " with tag " + this.dotted(ltuid.intValue()) );
        
        this.haltForRegistration();
    }
    
    private void haltForRegistration(){ }
    
    public void stepMeOut() {
        this.setStepping(this.currentTag(), false); // Clears the step status
    }
    
    public void untagCurrent(){
        currentguid.set(null);
    }
    
    public boolean isCurrentTagged(){
        return (currentguid.get() == null);
    }
    
    public int currentTag(){
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Retrieving tag.");
        
        Integer tgid = (Integer)currentguid.get();
        if(tgid == null)
            throw new IllegalStateException("Cannot retrieve a thread marker before it gets set");
        
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Retrieved tag " + tgid + " that corresponds to " + this.dotted(tgid.intValue()));
        
        return ((Integer)currentguid.get()).intValue();
    }
    
    public boolean isStepping(int uuid){
        if(getSetLogger.isDebugEnabled()) 
            getSetLogger.debug("Tagger.isStepping() called.");
        Byte state = stepping.get(uuid);
        if(state == null) return false;
        return state == DebuggerConstants.STEPPING_INTO;
    }
    
    public void setStepping(int uuid, boolean into){
        if(into == true)
            stepping.put(uuid, DebuggerConstants.STEPPING_INTO);
        else
            stepping.put(uuid, DebuggerConstants.STEPPING_OVER);
    }
    
    public void unsetStepping(int uuid){
        stepping.remove(uuid);
    }
    
    /** Makes the caller local thread part of the distributed thread
     * with ID 'uuid'.
     * 
     * @param uuid
     */
    public void makePartOf(int uuid){
        if(stepPartLogger.isDebugEnabled()){
            int id = this.currentTag();
            stepPartLogger.debug("Will BIND local thread " + this.dotted(id)
                    + " to distributed thread " + this.dotted(uuid));
        }
        Integer current = (Integer)partOf.get();
        if(current != null){
            stepPartLogger
                    .error("Local thread "
                            + dotted(currentTag()) + " is being marked twice as part of "
                            + ((current.intValue() == uuid) ? "the same distributed thread (NOT THAT BAD)"
                                    : "different distributed threads (THIS IS REALLY BAD)") + ".");
        }
        partOf.set(new Integer(uuid));
    }
    
    public void unmakePartOf(int uuid){
        if(stepPartLogger.isDebugEnabled()){
            int id = this.currentTag();
            stepPartLogger.debug("Will UNBIND local thread " + this.dotted(id)
                    + " from distributed thread " + this.dotted(uuid));
        }

        Integer current = (Integer)partOf.get();
        if(current == null){
            stepPartLogger
                    .error("Local thread "
                            + dotted(currentTag()) + " is being unmarked twice as part of "
                            + "distributed thread " + dotted(uuid) +".");
        }
    }
    
    public void retrieveStamp(){
        // NO-OP, we're doing this at the interceptor. I must change that. 
    }
    
    public Integer partOf(){
        Integer partUUID = (Integer)partOf.get();
        Integer myUUID = (Integer)currentguid.get();
        assert(partUUID != null || myUUID != null);
        
        return (partUUID == null)?myUUID:partUUID;
    }
    
    private String dotted(int uuidint){
        int lgid = uuidint & DebuggerConstants.LUID_MASK;
        int uid = (uuidint & DebuggerConstants.GID_MASK) >> 24;
        
        return(uid + "." + lgid); 
    }

}
