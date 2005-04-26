/*
 * Created on Apr 23, 2005
 * 
 * file: BranchKey.java
 */
package ddproto1.configurator.newimpl;

public class BranchKey{
    private String key;
    private String val;
    
    private int hashCode;
    
    public BranchKey(String key, String val){
        this.key = key;
        this.val = val;
        this.hashCode = (key + val).hashCode();
    }
    
    public String getKey() { return key; }
    
    public String getValue() { return val; }
    
    public int hashCode(){
        return hashCode;
    }
    
    public boolean equals(Object anObject){
        if(!(anObject instanceof BranchKey)) return false;
        BranchKey other = (BranchKey)anObject;
        return key.equals(other.key) && val.equals(other.val);
    }
}