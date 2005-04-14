/*
 * Created on Apr 14, 2005
 * 
 * file: ObjectSpecTypeImpl.java
 */
package ddproto1.configurator.newimpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// FIXME: RETAKE FROM HERE.
public class ObjectSpecTypeImpl implements IObjectSpecType{

    private String concreteType;
    private String interfaceType;
    
    // Static constraint storage.
    private Set<String> requiredAttributes = new HashSet<String>();
    private Map<String, Integer> children = new HashMap<String, Integer>();
    
    public ObjectSpecTypeImpl(String incarnableType, String type){
        this.interfaceType = type;
        this.concreteType = incarnableType;
    }
    
    public void addChild(String childtype, int multiplicity) {
        
        /* Dealing with twisted auto-boxing semantics. But it's cool. At
         * least cooler than manual boxing/unboxing. 
         */ 
        Integer existent = children.get(childtype);
        if(existent != null) existent += multiplicity;
        else existent = 0;
        
        children.put(childtype, existent);        
    }

    public boolean removeChild(String childtype) {
        return !(children.remove(childtype) == null);
    }

    public String getConcreteType() {
       return concreteType;
    }
    
    public String getInterfaceType(){
        return interfaceType;
    }

    public void addAttribute(String attributeKey) {
        // TODO Auto-generated method stub
        
    }

    public void removeAttribute(String attributeKey) {
        // TODO Auto-generated method stub
        
    }

    public void lockForReading() {
        // TODO Auto-generated method stub
        
    }

    public void lockForWriting() {
        // TODO Auto-generated method stub
        
    }

    public void unlock() {
        // TODO Auto-generated method stub
        
    }

    public Set<String> attributeSet() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean containsAttribute(String key) {
        // TODO Auto-generated method stub
        return false;
    }

    public IObjectSpec makeInstance() throws InstantiationException {
        // TODO Auto-generated method stub
        return null;
    }

    public void addOptionalSet(String attributeKey, String value, String specName) {
        // TODO Auto-generated method stub
        
    }

    public void removeOptionalSet(String attributeKey, String value) {
        // TODO Auto-generated method stub
        
    }

}
