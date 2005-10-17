/*
 * Created on Oct 17, 2005
 * 
 * file: ChildrenDFSIterator.java
 */
package ddproto1.configurator.newimpl;

import java.util.Iterator;

public class ChildrenDFSIterator extends AbstractDFSIterator{

    public ChildrenDFSIterator(IObjectSpec root){
        super(root);
    }
    
    @Override
    protected Iterator<Object> getChildren(Object parent) {
        if(parent instanceof IObjectSpec){
            IObjectSpec searchableVertex = (IObjectSpec) parent;
            return (Iterator)searchableVertex.getChildren().iterator();
        }else
            return null;
    }
    
}
