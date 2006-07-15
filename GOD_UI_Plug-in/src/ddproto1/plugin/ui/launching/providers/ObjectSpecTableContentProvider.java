/*
 * Created on Sep 3, 2005
 * 
 * file: ObjectSpecTableContentProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ddproto1.configurator.IAttribute;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;

public class ObjectSpecTableContentProvider implements IStructuredContentProvider{
    private IObjectSpec root;
    Object [] attributeCache = null;
    
    public IObjectSpec currentSpec(){
        return root;
    }

    public void dispose() { }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        root = (IObjectSpec) newInput;
        attributeCache = null;
    }

    public Object[] getElements(Object inputElement) {
        if(attributeCache != null) return attributeCache;
        
        IObjectSpec spec = (IObjectSpec) inputElement;
        IObjectSpecType specType = spec.getType();
        List<IAttribute> attributes = new ArrayList<IAttribute>();
        for(String attributeKey : spec.getAttributeKeys()){
            IAttribute attribute = specType.getAttribute(attributeKey);
            attributes.add(attribute);
        }
        
        attributeCache = attributes.toArray();
        return attributeCache;
    }
}
