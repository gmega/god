/*
 * Created on Aug 17, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: MethodSearcher.java
 */

package ddproto1.util;

import java.lang.reflect.Method;

/**
 * @author giuliano
 *
 */
public class MethodSearchUtil {
    private static MethodSearchUtil instance;
    
    public static synchronized MethodSearchUtil getInstance(){
        return (instance == null)?(instance = new MethodSearchUtil()):instance;
    }
    
    public Method searchHierarchy(Class sClass, String name, Class [] params, int depth){
        
        if(depth == 0 || sClass == null) return null;
        
        Method m = null;
                
        /* If the class modifier is not compatible we don't even bother checking
           it */
        // First, searches the class itself
        try {
            m = sClass.getDeclaredMethod(name, params);
            if (compareParameters(m, params))
                return m;
        } catch (NoSuchMethodException e) { }
            

        if (m == null) {
            // If not found, search the interfaces
            Class[] intfs = sClass.getInterfaces();
            for (int i = 0; i < intfs.length; i++) {
                try {
                    m = intfs[i].getDeclaredMethod(name, params);
                    if (compareParameters(m, params))
                        return m;
                } catch (NoSuchMethodException ex) { }
            }
        }
        

        if (m == null) {
            /*
             * If can't find the method on the interfaces, start all over again
             * with the superclass
             */
            m = searchHierarchy(sClass.getSuperclass(), name, params,
                    depth - 1);
        }

        return m;
    }
    
    public boolean compareParameters(Method m, Class [] par2){
        Class [] par1 = m.getParameterTypes();
        int par2length = (par2 == null)?0:par2.length;
        if(par1.length != par2length) return false;
        for(int i = 0; i < par1.length; i++){
            if(!par1[i].equals(par2[i])) return false;
        }
        
        return true;
    }
}
