/*
 * Created on Aug 27, 2005
 * 
 * file: DFSAttributeLookup.java
 */
package ddproto1.configurator.newimpl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public class DFSAttributeLookup implements IAttributeLookupAlgorithm{

    public Iterable<IObjectSpec> iterableWithRoot(IContextSearchable root) {
        
        final IContextSearchable _root = root;
        
        return new Iterable<IObjectSpec>(){
            public Iterator<IObjectSpec> iterator() {
                return new DFSIterator(_root);
            }
        };
    }
    
    private class DFSIterator implements Iterator<IObjectSpec>{
        
        private Stack <Iterator<IObjectSpec>> stack = new Stack<Iterator<IObjectSpec>>();
        private Set <IObjectSpec> visited = new HashSet<IObjectSpec>();
        
        public DFSIterator(IContextSearchable root){
            stack.push(root.getAllParents().iterator());
        }

        public boolean hasNext() {
            if(stack.isEmpty()) return false;

            Iterator<IObjectSpec> edges = stack.peek();
            
            /** Pops off iterators until there's one that still contains
             * vertexes to process.
             */
            while(!edges.hasNext()) stack.pop();
            return !stack.isEmpty();
        }

        public IObjectSpec next() {
            if(!hasNext()) throw new NoSuchElementException();
            
            IObjectSpec vertex = stack.peek().next();
            if(visited.contains(vertex)) throw new IllegalStateException("Heterarchy graph contains cycles.");
            
            /** If the object spec is not context searchable, we cannot include 
             * its parents in the DFS.
             */
            if(vertex instanceof IContextSearchable){
                IContextSearchable searchableVertex = (IContextSearchable) vertex;
                stack.push(searchableVertex.getAllParents().iterator());
            }
            
            visited.add(vertex);
            
            return vertex;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }

}
