/*
 * Created on Sep 1, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IClassLoadingHook.java
 */

package ddproto1.localagent.instrumentation.bcel;

import org.apache.bcel.classfile.JavaClass;

/**
 * @author giuliano
 *
 */
public interface IClassLoadingHook {
    public JavaClass modifyClass(JavaClass jc, ClassLoader loader);
}
