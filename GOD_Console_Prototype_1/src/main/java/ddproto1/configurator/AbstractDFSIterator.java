/*
 * Created on Oct 17, 2005
 * 
 * file: AbstractDFSIterator.java
 */
package ddproto1.configurator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

public abstract class AbstractDFSIterator implements Iterator<Object>{
    private Stack<Iterator<Object>> stack = new Stack<Iterator<Object>>();

    private Set<Object> visited = new HashSet<Object>();

    public AbstractDFSIterator(Object root) {
        stack.push(this.getChildren(root));
    }

    public boolean hasNext() {
        if (stack.isEmpty())
            return false;

        Iterator<Object> edges;

        /**
         * Pops off iterators until there's one that still contains vertexes
         * to process.
         */
        do{
            edges = stack.peek();
            if(edges.hasNext()) break;
            stack.pop();
        }while(!stack.isEmpty());
        
        return !stack.isEmpty();
    }

    public Object next() {
        if (!hasNext())
            throw new NoSuchElementException();

        Object vertex = stack.peek().next();
        if (visited.contains(vertex))
            throw new IllegalStateException("Heterarchy graph is not a tree.");

        /** Pushes the children iterator onto the stack. */
        Iterator <Object> it = this.getChildren(vertex);
        if(it == null) throw new ClassCastException("Heterarchy contains vertexes " +
                "of unsupported types.");
        stack.push(it);
        
        visited.add(vertex);

        return vertex;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    protected abstract Iterator<Object> getChildren(Object parent);

}
