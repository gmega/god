/*
 * Created on Nov 25, 2004
 * 
 * file: RunnableHook.java
 */
package ddproto1.localagent.instrumentation.bcel;

import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;

/**
 * @author giuliano
 *
 * 
 */
public class BCELRunnableHook implements IClassLoadingHook{

    private static final Logger logger = Logger.getLogger(BCELRunnableHook.class);
    
    /** This works because Runnable is accessible through the bootstrap classloader. 
     * No chances of mess because we're also loaded by the bootstrap classloader. */
    private static final Class runnableIntf = Runnable.class;
    
    private String [] filteredPackages;
    private boolean allowed;
    
    public BCELRunnableHook(Set<String> packageFilter, boolean allowed){
        this.allowed = allowed;
        filteredPackages = new String[packageFilter.size()];
        packageFilter.toArray(filteredPackages);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.localagent.IClassLoadingHook#modifyClass(org.apache.bcel.classfile.JavaClass)
     */
    public JavaClass modifyClass(JavaClass jc, ClassLoader cl) {
        
        /** Don't refactor this expression. It's important that unallowed classes
         * don't go to isInstanceOfRunnable. 
         */
        if(!this.isAllowed(jc) || !this.isInstanceOfRunnable(jc, cl)) return jc;

        Method [] methods = jc.getMethods();
        int i = 0;
        for(i = 0; i < methods.length; i++){
            if(methods[i].getName().equals(DebuggerConstants.RUN_METHOD_NAME))
               break; 
        }
        
        if(i == methods.length)
            throw new InternalError("Could not find method " + DebuggerConstants.RUN_METHOD_NAME 
                    + " in class " + jc.getClassName());
        
        ClassGen newClass = new ClassGen(jc);
        Method mt = glueSnippet(newClass, methods[i]);
        newClass.replaceMethod(methods[i], mt);
        
        return newClass.getJavaClass();
    }
    
    private Method glueSnippet(ClassGen nc, Method m){
        if(logger.isDebugEnabled()) 
            logger.debug("Instrumenting " + nc.getClassName() + " with runnable hook.");
        
        InstructionList il = new InstructionList();
        InstructionFactory iFactory = new InstructionFactory(nc);
        il.append(iFactory.createInvoke("ddproto1.localagent.Tagger", "getInstance", new ObjectType("ddproto1.localagent.Tagger"), Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(iFactory.createInvoke("ddproto1.localagent.Tagger", "tagCurrent", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                        
        MethodGen mg = new MethodGen(m, nc.getClassName(), nc.getConstantPool());
        
        InstructionList old_il = mg.getInstructionList();
        InstructionHandle [] handles = old_il.getInstructionHandles();
        old_il.insert(handles[0], il);
        
        mg.setInstructionList(old_il);
        
        mg.setMaxStack();
        mg.setMaxLocals();
        
        return mg.getMethod();
    }
    
    private boolean isAllowed(JavaClass jc){
        String _package = jc.getPackageName();
        for(String filteredPackage : filteredPackages)
            if(_package.startsWith(filteredPackage)) return allowed;
        
        return !allowed;
    }
    
    private boolean isInstanceOfRunnable(JavaClass clazzy, ClassLoader cl){
        
        /** Check superclasses */
        Class<?> c = null;
        try{
            c = Class.forName(clazzy.getSuperclassName(), true, cl);
        }catch(ClassNotFoundException ex){
            throw new RuntimeException("Internal assertion failed.");
        }
        if(runnableIntf.isAssignableFrom(c)) return true;
        
        for(String superClass : clazzy.getInterfaceNames()){
            try{
                c = Class.forName(superClass, true, cl);
            }catch(ClassNotFoundException ex){
                throw new RuntimeException("Internal assertion failed.");
            }
            if(runnableIntf.isAssignableFrom(c)) return true;
        }
        
        return false;
    }

}
