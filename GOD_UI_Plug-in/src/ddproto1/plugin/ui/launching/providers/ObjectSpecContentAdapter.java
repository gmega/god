/*
 * Created on Sep 3, 2005
 * 
 * file: ObjectSpecContentAdapter.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ddproto1.configurator.IContextSearchable;
import ddproto1.configurator.IObjectSpec;

public class ObjectSpecContentAdapter implements ITreeContentProvider{
    
    private IObjectSpec root;
    public static Object virtualRoot = new Object();
    
    /**
     * Children of a object specification might be:
     * 
     * 1) Other object specifications.
     * 2) Attributes
     * 
     */
    public Object[] getChildren(Object parentElement) {
        IObjectSpec spec = (IObjectSpec)parentElement;
        return spec.getChildren().toArray();
    }

    public Object getParent(Object element) {
        /** Keeps our dirty little secret consistent. */
        if(element == root) return virtualRoot;
        if(element == virtualRoot) return null;
        
        /** Object specs might have multiple parents, but we'll assume
         * there is only one since in the first version of the configurator
         * this is actually correct. 
         * 
         * If the spec is not context searchable, the graphical representation
         * will be crippled. I suppose that's better than a ClassCastException.      */
        if(!(element instanceof IContextSearchable)) return null;
        IContextSearchable spec = (IContextSearchable)element;
        Set<IObjectSpec> parents = spec.getAllParents();
        Iterator<IObjectSpec> it = parents.iterator();
        if(it.hasNext()) return it.next();
        
        return null;
    }   

    public boolean hasChildren(Object element) {
        IObjectSpec spec = (IObjectSpec)element;
        return !spec.getChildren().isEmpty();
    }

    public Object[] getElements(Object inputElement) {
        if(inputElement == virtualRoot) return new Object[] { root };
        return this.getChildren(inputElement);
    }

    public void dispose() {
        //NO-OP
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if(newInput == virtualRoot) return;
        this.root = (IObjectSpec) newInput;
    }

}
