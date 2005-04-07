/*
 * Created on Mar 22, 2005
 * 
 * file: CollectionTest.java
 */
package ddproto1.test;

import java.util.Iterator;
import java.util.LinkedList;

public class CollectionTest {

     /**
     * @param args
     */
    public static void main(String[] args) {
        LinkedList <Object> lobj = new LinkedList<Object>();

        for(int i = 0; i < 5; i++)
            lobj.add(new Object());
        
        Iterator<Object> it = lobj.iterator();
        while(it.hasNext()){
            Object obj = it.next();
            it.remove();
            System.out.println(lobj.size());
        }
        
    }

}
