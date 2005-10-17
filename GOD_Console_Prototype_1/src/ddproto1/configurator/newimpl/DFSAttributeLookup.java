/*
 * Created on Aug 27, 2005
 * 
 * file: DFSAttributeLookup.java
 */
package ddproto1.configurator.newimpl;

import java.util.Iterator;

public class DFSAttributeLookup implements IAttributeLookupAlgorithm{

    public Iterable<IObjectSpec> iterableWithRoot(IContextSearchable root) {
        
        final IContextSearchable _root = root;
        
        return new Iterable<IObjectSpec>(){
            public Iterator<IObjectSpec> iterator() {
                return new Iterator<IObjectSpec>(){
                    Iterator <Object> delegate = new ParentDFSIterator(_root);
                    public boolean hasNext() { return delegate.hasNext(); }
                    public IObjectSpec next() { return (IObjectSpec)delegate.next(); }
                    public void remove() { delegate.remove(); }
                };
            }
        };
    }
}
