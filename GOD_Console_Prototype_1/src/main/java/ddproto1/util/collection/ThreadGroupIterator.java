/*
 * Created on Aug 17, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ThreadGroupIterator.java
 */

package ddproto1.util.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import com.sun.jdi.ThreadGroupReference;

/**
 * @author giuliano
 *
 */
public class ThreadGroupIterator implements Iterator{

    private Stack <Iterator> stk = new Stack<Iterator>();
    
    public ThreadGroupIterator(List tlg){
        stk.push(tlg.iterator());
    }
    
    public ThreadGroupIterator(ThreadGroupReference tgr){
        List tlg = new ArrayList();
        stk.push(tlg.iterator());
    }
  
    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        do{
            Iterator current = (Iterator)stk.peek();
            if(!current.hasNext()) stk.pop();
            else break;
        }while(!stk.empty());
        
        return !stk.empty();
    }
    
    public int currentLevel(){
        return stk.size() - 1;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if(!hasNext()) throw new NoSuchElementException();
        Iterator current = (Iterator)stk.peek();
        ThreadGroupReference next = (ThreadGroupReference)current.next();
        List children = next.threadGroups();
        if(children != null){
            // Guarantees that parent iterators won't be popped before its children
            current = children.iterator();
            stk.push(current);
        }
        
        return next;
    }

}
