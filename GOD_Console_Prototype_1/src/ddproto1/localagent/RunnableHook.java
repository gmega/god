/*
 * Created on Nov 25, 2004
 * 
 * file: RunnableHook.java
 */
package ddproto1.localagent;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import ddproto1.commons.DebuggerConstants;

/**
 * @author giuliano
 *
 */
public class RunnableHook implements IClassLoadingHook{

    private static JavaClass runnableIntf;
    
    /* (non-Javadoc)
     * @see ddproto1.localagent.IClassLoadingHook#modifyClass(org.apache.bcel.classfile.JavaClass)
     */
    public JavaClass modifyClass(JavaClass jc) {
        RunnableHook.getRunnableClass();
        
        if(!jc.instanceOf(runnableIntf)) return jc;

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
        newClass.removeMethod(methods[i]);

        Method mt = glueSnippet(newClass, methods[i]);
        
        newClass.addMethod(mt);
        
        return newClass.getJavaClass();
    }
    
    private static void getRunnableClass()
    {
        if(runnableIntf != null) return;
        
        runnableIntf = Repository.lookupClass(DebuggerConstants.RUNNABLE_INTF_NAME);
        if(runnableIntf == null)
            throw new InternalError("The " + DebuggerConstants.RUNNABLE_INTF_NAME + 
                    " interface could not be retrieved."); 
    }
    
    private Method glueSnippet(ClassGen nc, Method m){
        
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

}
