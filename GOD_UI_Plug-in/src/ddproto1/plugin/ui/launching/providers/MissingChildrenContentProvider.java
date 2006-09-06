/*
 * Created on Sep 4, 2005
 * 
 * file: MissingChildrenContentProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ddproto1.configurator.IIntegerInterval;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IntegerIntervalImpl;

public class MissingChildrenContentProvider implements IStructuredContentProvider{

    private Map<String, IIntegerInterval> intervalCache = new HashMap<String, IIntegerInterval>();
    private List<String> keyCache = new ArrayList<String>();
    private boolean keyCacheIsValid = false;
    
    private IObjectSpec current;
    
    public Object[] getElements(Object inputElement) {
        return this.getKeyCache().toArray();
    }
    
    public IIntegerInterval getFromCache(String key){
        return intervalCache.get(key);
    }

    public void dispose() { }
    
    private List<String> getKeyCache(){
        if(keyCacheIsValid) return keyCache;
        Map<String, Integer> missingChildren = current.getMissingChildren();
        
        for(String key : missingChildren.keySet()){
            int maxAccepted = missingChildren.get(key);
            int minRequired = maxAccepted;
            if(maxAccepted == 0)
                maxAccepted = current.allowedChildrenOfType(key);
            if(maxAccepted == 0) continue;
            keyCache.add(key);
            intervalCache.put(key, new IntegerIntervalImpl(minRequired, maxAccepted));
        }
        keyCacheIsValid = true;
        return keyCache;
    }
    
    public void flushCache(){
        if(current == null) return;
        intervalCache.clear();
        keyCache.clear();
        keyCacheIsValid = false;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.current = (IObjectSpec)newInput;
        this.flushCache();
    }

}
