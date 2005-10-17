/*
 * Created on Oct 17, 2005
 * 
 * file: DFSIterator.java
 */
package ddproto1.configurator.newimpl;

import java.util.Iterator;

public class ParentDFSIterator extends AbstractDFSIterator {

    public ParentDFSIterator(IContextSearchable root){
        super(root);
    }
    
    @Override
    protected Iterator<Object> getChildren(Object parent) {
        if (parent instanceof IContextSearchable) {
            IContextSearchable searchableVertex = (IContextSearchable) parent;
            /** Casts it back to conform to the interface. */
            return (Iterator)searchableVertex.getAllParents().iterator();
        }else
            return null;
    }

}
