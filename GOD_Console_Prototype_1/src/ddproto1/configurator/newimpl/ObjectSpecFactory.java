/*
 * Created on Oct 17, 2005
 * 
 * file: ObjectSpecTypeMementoFactory.java
 */
package ddproto1.configurator.newimpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.util.collection.UnorderedMultiMap;

/**
 * This auxiliary object transforms IObjectSpecs into Strings
 * and vice-versa.  
 * 
 * @author giuliano
 *
 */
public class ObjectSpecFactory {
    public ObjectSpecFactory(SpecLoader loader){
        
    }
    
    public IObjectSpec restoreFromString(String spec){
        
    }
    
    public String makeFromObjectSpec(IObjectSpec spec){
        if(!(spec instanceof IContextSearchable))
            throw new UnsupportedException("Cannot transform non-context-searchable " +
                    "object specs into strings.");
            
        UnorderedMultiMap<Integer, Integer> childMap = new UnorderedMultiMap<Integer, Integer>(
                HashSet.class);  
        
        Map<IObjectSpec, Integer> spec2Id = new HashMap<IObjectSpec, Integer>();
        
        /** First we build the child map. */
        Iterator it = new ChildrenDFSIterator(spec);
        int id = 0;
        spec2Id.put(spec, id);
        
        while(it.hasNext()){
            IObjectSpec next = (IObjectSpec)it.next();
            int pId = spec2Id.get(next);
            for(IObjectSpec child : next.getChildren()){
                int cId;
                if(spec2Id.containsKey(child)) cId = spec2Id.get(child);
                else{
                    cId = ++id;
                    spec2Id.put(child, cId);
                }
                childMap.add(pId, cId);
            }
        }

        StringBuffer stringForm = new StringBuffer();
        /** Now we build the actual string representation. First the definitions.*/
        for(IObjectSpec cSpec : spec2Id.keySet()){
            stringForm.append(spec2Id.get(cSpec));
            for(String attKey : cSpec.getAttributeKeys()){
                String val;
                try {
                    val = cSpec.getAttribute(attKey);
                    stringForm.append(IConfigurationConstants.LIST_SEPARATOR_CHAR);
                    stringForm.append(attKey);
                    stringForm.append("=");
                    stringForm.append(val);
                } catch (IllegalAttributeException e) {
                    throw new InternalError("IObjectSpec reported supporting an attribute " +
                            "it doesn't. Concurrent modification?");
                } catch (UninitializedAttributeException e) { }
            }
        }
        
        /** Now the child map. */
        stringForm.append(IConfigurationConstants.LIST_SEPARATOR_CHAR);
        
    }
   
}
