/*
 * Created on Mar 22, 2005
 * 
 * file: StdTypeImpl.java
 */
package ddproto1.debugger.request;

public class StdTypeImpl implements IDeferrableRequest.IPreconditionType{

    
    public static final int CLASSLOADING = 0;
    public static final int VM_CONNECTION = 1;
    public static final int THREAD_PROMOTION = 2; 
    public static final int NIL = -1;
    
    public static final int MATCH_ONCE = 0;
    public static final int MATCH_MULTIPLE = 1;

    private static final String[] events = { "NIL", "CLASSLOADING",
            "VM_CONNECTION", "THREAD_PROMOTION" };
    
    private static final String [] match = {"MATCH_ONCE", "MATCH_MULTIPLE"};
    
    private int eventType;
    private int matchType;
    
    public StdTypeImpl(int eventType, int matchType) {
        this.eventType = eventType;
        this.matchType = matchType;
    }

    public int eventType() {
        return eventType;
    }

    public int matchType() {
        return matchType;
    }
    
    public String toString(){
        return "(event: " + events[eventType+1] + ", match: " + match[matchType] + ")";
    }
    
    public boolean equals(Object other){
        if(!(other instanceof StdTypeImpl)) return false;
        
        StdTypeImpl otherType = (StdTypeImpl) other;
        
        return((otherType.eventType == eventType) && (otherType.matchType == matchType));
    }
    
    public int hashCode(){
        return eventType + matchType;
    }

}
