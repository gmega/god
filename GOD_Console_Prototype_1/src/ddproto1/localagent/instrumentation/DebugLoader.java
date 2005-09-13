/*
 * Created on Sep 1, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DebugLoader.java
 */

package ddproto1.localagent.instrumentation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassLoader;


/**
 * @author giuliano
 *
 */
public class DebugLoader extends ClassLoader{
    
    private Set hooks = new HashSet();
    
    public DebugLoader(){}
    
    public DebugLoader(String [] ignored){
        super(ignored);
    }

    protected JavaClass modifyClass(JavaClass clazz) {
	
		/*
		 * While this approach may sound innapropriate, I personally
		 * believe it to be a better option than hard-coding if statements
		 * and instrumentation methods to the loader's body. This way we
		 * can add new instrumentation methods by simply implementing them
		 * and then registering here.  
		 * 
		 * Note: This approach does not encompass those situations when there
		 * are dependencies among hooks. Those have to be modelled externally 
		 * (another thing that I find reasonable).
		 */
		Iterator it = hooks.iterator();
		JavaClass jcl = clazz;
		
		while(it.hasNext()){
		    IClassLoadingHook iclh = (IClassLoadingHook)it.next();
		    jcl = iclh.modifyClass(jcl); 
		}
		
		return jcl;
    }
    
    public void registerHook(IClassLoadingHook iclh){
        hooks.add(iclh);
    }
    
    public boolean unregisterHook(IClassLoadingHook iclh){
        return hooks.remove(iclh);
    }
}
