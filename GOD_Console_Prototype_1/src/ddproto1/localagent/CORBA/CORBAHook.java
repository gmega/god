/*
 * Created on Sep 1, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: CORBAHook.java
 */

package ddproto1.localagent.CORBA;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Type;

import ddproto1.commons.DebuggerConstants;
import ddproto1.localagent.IClassLoadingHook;

/**
 * @author giuliano
 *
 */
public class CORBAHook implements IClassLoadingHook, DebuggerConstants{

    private static final String ORBHOLDER_NAME = "ddproto1.localagent.CORBA.ORBHolder";
    
    private static final boolean DEBUG_MODE = false;
    private static final String DUMP_DIR = "/home/giuliano/workspace/Distributed Debugger Prototype 1/runtime-gen";
    
    private Map <String, ObjectType> stubs;
    private Map <String, ObjectType> skeletons;
    
    public CORBAHook(ObjectType [] clientSide, ObjectType [] serverSide){
        stubs = new HashMap<String, ObjectType>();
        skeletons = new HashMap<String, ObjectType>();
        
        /* We could actually provide a more flexible specification
         * through pluggable "glueSnippets", but I don't think
         * we'll need more differentiation among CORBA classes than 
         * what we already have. Besides, we can always plug new 
         * functionality by installing new hooks to the class loader. 
         */
        for(int i = 0; i < clientSide.length; i++){
        	stubs.put(clientSide[i].getClassName(), clientSide[i]);
        }
        
        for(int i = 0; i < serverSide.length; i++){
            skeletons.put(serverSide[i].getClassName(), serverSide[i]);
        }
    }
    
    /* (non-Javadoc)
     * @see ddproto1.localagent.IClassLoadingHook#modifyClass(org.apache.bcel.classfile.JavaClass)
     */
    public JavaClass modifyClass(JavaClass jc) {
        String name = jc.getClassName();
        
        boolean serverClass;
        String finallyName = null;
        
        if(stubs.containsKey(name)) serverClass = false;
    	else if(skeletons.containsKey(name)) serverClass = true;
    	else return jc;
    	
   	Method [] methods = jc.getMethods();
    	
    	ClassGen cgen = new ClassGen(jc);
    	ConstantPoolGen cpg = cgen.getConstantPool();
    	boolean modified = false;
        
        for(int i = 0; i < methods.length; i++){

            MethodGen mg = new MethodGen(methods[i], cgen.getClassName(), cpg);
            Code code = methods[i].getCode();
           
            int modifiers = methods[i].getAccessFlags();
            
            /* Exclude constructors and static initializers */
            String mname = methods[i].getName();
            if(mname.startsWith("<"))
                continue;
            
            /* Do the "sanity check" (as bcel examples like to call) */
            if (((Constants.ACC_NATIVE & modifiers) != 0)
                    | ((Constants.ACC_NATIVE & modifiers) != 0) | (code == null)) 
                continue;
            
            // Remove old method
            cgen.removeMethod(methods[i]);
            
            if(finallyName == null) finallyName = this.getFinallyName(cgen);
            
            if(!serverClass) glueStubSnippet(mg, cgen, finallyName);
            else glueSkeletonSnippet(mg, cgen, finallyName);
           
            mg.setMaxLocals();
            mg.setMaxStack();
            
            cgen.addMethod(mg.getMethod());
            
            modified = true;
          }
        
        if(modified && serverClass) this.addServerFinallyRoutine(cgen, finallyName);
        else if(modified && !serverClass) this.addClientFinallyRoutine(cgen, finallyName);
        
        if(DEBUG_MODE && modified){
            writeOut(cgen.getJavaClass(), DUMP_DIR);
        }
        
    	return (modified)?cgen.getJavaClass():jc;
    }
   
    private void writeOut(JavaClass cg, String file){
        try{
            String name = cg.getClassName();
            int last = name.lastIndexOf('.');
            if(last != -1){
                name = name.substring(last+1, name.length());
            }
            cg.dump(new File(file + "/" + name));
        }catch(Exception e){ }
    }
    
    /**
     * Glues the server-side code to all skeleton methods.
     * 
     * Basically what we perform is this:
     * 
     * original method: 
     * 
     * ... method ( ... ) throws ...,...{
     *      [SOURCE TEXT BODY]
     * }
     * 
     * transformed:
     * 
     * ... method ( ... ) throws ...,...{
     *      try{
     *          ORBHolder.getInstance().retrieveStamp(); ----------------> starting hook
     *          [SOURCE TEXT BODY]
     *      }finally{
     *          ORBHolder.getInstance().checkOut(); ---------------------> finishing hook
     *      }
     * }
     *  
     *  
     * 
     * @param mg
     * @param cgen
     */
     
    private void glueSkeletonSnippet(MethodGen mg, ClassGen cgen, String finallyName){
        
        InstructionFactory iFactory = new InstructionFactory(cgen);
        
        /**
         *  This is our start hook - it consists of a call to the debug library
         *  which should mark the thread servicing the request at the instrumented skeleton
         *  with an appropriate unique ID.
         */
        InstructionList startHook = new InstructionList();
        startHook.append(iFactory.createInvoke("ddproto1.localagent.CORBA.ORBHolder", "getInstance", new ObjectType("ddproto1.localagent.CORBA.ORBHolder"), Type.NO_ARGS, Constants.INVOKESTATIC));
        startHook.append(iFactory.createInvoke("ddproto1.localagent.CORBA.ORBHolder", "retrieveStamp", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        
        this.wrapFinally(iFactory, startHook, mg, cgen, finallyName);
    }
    
    private void glueStubSnippet(MethodGen mg, ClassGen cgen, String finallyName){
        
        InstructionList il = new InstructionList();
        InstructionFactory iFactory = new InstructionFactory(cgen);
        il.append(iFactory.createInvoke("ddproto1.localagent.CORBA.ORBHolder", "getInstance", new ObjectType("ddproto1.localagent.CORBA.ORBHolder"), Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(iFactory.createInvoke("ddproto1.localagent.CORBA.ORBHolder", "setStamp", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

        // Wrap the method into a thread-trapping mechanism.
        this.wrapFinally(iFactory, il, mg, cgen, finallyName);
    }
    
    private void wrapFinally(InstructionFactory iFactory, InstructionList startHook, MethodGen mg, ClassGen cgen, String finallyName){
        InstructionHandle first = startHook.getStart();
        
        /* Calls the finally block in case there's an exception, then rethrows that exception . */
        int handler_reg = mg.getMaxLocals() + 1;    // Safe register to store exception handler stuff.
        InstructionList endHook = new InstructionList();
        InstructionHandle handler_start = endHook.append(new ASTORE(handler_reg));   // Stores the throwable
        endHook.append(new ALOAD(0));
        
        /*
         * Old line was:
         * 
         * InstructionHandle last = endHook.append(iFactory.createInvoke(cgen.getClassName(), finallyName, 
         *          Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
         * 
         * WRONG! 'last' can't be this line or we'll enclose the finally block into the handler, leading to an
         * infinite loop in case the finally block produces an exception:
         * 
         * finally block throws exception
         * exception handler handles exception
         * exception handler calls finally block
         * finally block throws exception
         * ... ad infinitum ...
         * 
         */

        endHook.append(iFactory.createInvoke(cgen.getClassName(), finallyName, Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        endHook.append(new ALOAD(handler_reg));
        endHook.append(new ATHROW());
        
        /* Glues the stuff together.*/
        InstructionList methodCode = mg.getInstructionList();
        methodCode.insert(methodCode.getStart(), startHook);
        InstructionHandle last = methodCode.getEnd();
        methodCode.append(methodCode.getEnd(), endHook);
        
        /* Insert calls to the finally block before each return instruction 
         * so that it gets called in even if there is no exception. */
        Iterator it = methodCode.iterator();
        while(it.hasNext()){
            InstructionHandle ih = (InstructionHandle)it.next();
            if(ih.getInstruction() instanceof ReturnInstruction){
                methodCode.insert(ih, new ALOAD(0));
                methodCode.insert(ih, iFactory.createInvoke(
                        cgen.getClassName(), finallyName, Type.VOID,
                        Type.NO_ARGS, Constants.INVOKEVIRTUAL));

            }
        }
        
        /* Creates the exception handler */
        mg.addExceptionHandler(first, last, handler_start, null);
        
        mg.setMaxStack();
        mg.setMaxLocals();
        
    }
    
    private void addServerFinallyRoutine(ClassGen cgen, String finallyName){
        InstructionFactory iFactory = new InstructionFactory(cgen.getConstantPool());
        /* Subroutine that defines the finally block - it contains our finishing hook*/
        InstructionList finallyBl = new InstructionList();
        finallyBl.append(iFactory.createInvoke(ORBHOLDER_NAME, INSTANCE_GETTER, new ObjectType(ORBHOLDER_NAME), Type.NO_ARGS, Constants.INVOKESTATIC));
        finallyBl.append(iFactory.createInvoke(ORBHOLDER_NAME, "checkOut", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        finallyBl.append(InstructionFactory.createReturn(Type.VOID));
        MethodGen finallyMethod = new MethodGen(Constants.ACC_PRIVATE, Type.VOID, Type.NO_ARGS, new String [] { }, finallyName, cgen.getClassName(), finallyBl ,cgen.getConstantPool());
        
        finallyMethod.setMaxLocals();
        finallyMethod.setMaxStack();
        
        cgen.addMethod(finallyMethod.getMethod());
    }
    
    /**
     * Client-side finally block:
     * 
     * Tagger tagger = Tagger.getInstance();
     * if(tagger.isStepping(tagger.currentTag()){
     *    tagger.stepMeOut();
     * }
     * return;
     * 
     * @param cgen
     * @param finallyName
     */
    private void addClientFinallyRoutine(ClassGen cgen, String finallyName){
        InstructionFactory iFactory = new InstructionFactory(cgen.getConstantPool());
        /* Creates the client-side finally block subroutine. */
        InstructionList finallyBl = new InstructionList();
        finallyBl.append(iFactory.createInvoke(TAGGER_CLASS_NAME,
                INSTANCE_GETTER, new ObjectType(TAGGER_CLASS_NAME),
                Type.NO_ARGS, Constants.INVOKESTATIC));
        finallyBl.append(new ASTORE(1));
        
        // TAGGER
        finallyBl.append(new ALOAD(1));
        // TAGGER TAGGER
        finallyBl.append(new ALOAD(1));
        // TAGGER (TAGGER currentTag) --> TAGGER INT
        finallyBl.append(iFactory.createInvoke(TAGGER_CLASS_NAME, UUID_GETTER, Type.INT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        // (TAGGER INT isStepping) --> BOOLEAN
        finallyBl.append(iFactory.createInvoke(TAGGER_CLASS_NAME, STEP_GETTER, Type.BOOLEAN, new Type[] {Type.INT}, Constants.INVOKEVIRTUAL));

        // if(tagger.isStepping(tagger.currentTag()){ tagger.stepMeOut(); } return;
        BranchInstruction toEnd = InstructionFactory.createBranchInstruction(Constants.IFEQ, null);
        finallyBl.append(toEnd);
        finallyBl.append(new ALOAD(1));
        
        // Traps the thread to the special place.
        finallyBl.append(iFactory.createInvoke(TAGGER_CLASS_NAME, "stepMeOut", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        toEnd.setTarget(finallyBl.append(InstructionFactory.createReturn(Type.VOID)));
        finallyBl.append(InstructionFactory.createReturn(Type.VOID));
        
        MethodGen finallyMethod = new MethodGen(Constants.ACC_PRIVATE, Type.VOID, Type.NO_ARGS, new String [] {}, finallyName, cgen.getClassName(), finallyBl, cgen.getConstantPool());
        
        finallyMethod.setMaxLocals();
        finallyMethod.setMaxStack();
        
        cgen.addMethod(finallyMethod.getMethod());
    }
    
    private String getFinallyName(ClassGen cg){
        /* There will hardly be a name clash with this, but we could
         * implement a random based, clash-aware generator if we wanted
         * to.
         */
        return "finally_block_ddproto1_localagent";
    }
}
