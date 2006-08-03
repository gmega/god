/*
 * Created on Aug 18, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: StdPreconditionImpl.java
 */

package ddproto1.debugger.request;

/**
 * Though it might seem unnecessary to have a whole public class implementing
 * such simple behaviour, we saw the need for this as we began using IPrecondition
 * objects as indexes into HashMaps. As you may be well aware of, a disagreement 
 * between hashcodes renders HashMaps and HashSets unusable, even if keys implement
 * the same interface.
 * 
 * @author giuliano
 *
 */
public class StdPreconditionImpl implements IDeferrableRequest.IPrecondition{
    private String classId;
    private StdTypeImpl ipt;
    
    public void setType(StdTypeImpl ipt){
        this.ipt = ipt;
    }
    
    public void setClassId(String clsid){
        classId = clsid;
    }
        
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest.IPrecondition#getType()
     */
    public IDeferrableRequest.IPreconditionType getType() {
        return ipt;
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest.IPrecondition#getClassId()
     */
    public String getClassId() {
        return classId;
    }
    
    public String toString(){
        return "[Type:" + ipt + ", Class ID:" + classId + "]"; 
    }
    
    public boolean equals(Object other){
        IDeferrableRequest.IPrecondition ip;
        try{
            ip = (IDeferrableRequest.IPrecondition)other;
        }catch(ClassCastException e){
            return false;
        }
        
        return ((classId == null)?(ip.getClassId() == null):(classId.equals(ip.getClassId()))) 
        			&& ipt.equals(ip.getType());
    }
    
    public int hashCode(){
        /* I have no idea whether this hash function produces evenly spread
         * stuff or not. I suppose it does. 
         */
        return ((classId == null)?0:classId.hashCode()) + ipt.hashCode();
    }
}
