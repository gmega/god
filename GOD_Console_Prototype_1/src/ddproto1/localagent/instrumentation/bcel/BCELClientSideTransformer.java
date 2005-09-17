/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.bcel;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.log4j.Logger;


public class BCELClientSideTransformer implements ClassFileTransformer{
    
    private List<IClassLoadingHook> modifiers = new ArrayList<IClassLoadingHook>();
    Logger logger = Logger.getLogger(BCELClientSideTransformer.class);
        
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try{
            if(logger.isDebugEnabled()) logger.debug("Parsing " + className);
            /** Creates a BCEL class file and delegates processing to its hooks. 
             * I can't really assert the name of the source file for this class, so I pass
             * the "garbage" string. I think BCEL doesn't use it, but if it does then I better 
             * maximize the chances of provoking some grotesque error that can be caught. 
             * Its better than having some bizarre behaviour and not knowing why. */
            ClassParser cp = new ClassParser(new ByteArrayInputStream(classfileBuffer), "garbage");
            JavaClass clazz = cp.parse();
            
            for(IClassLoadingHook modifier : modifiers){
                clazz = modifier.modifyClass(clazz, loader);
            }
            
            return clazz.getBytes();
        }catch(Throwable t){
            logger.error("Failed instrumenting class " + className, t);
            return classfileBuffer;
        }
    }
    
    public void addModifier(IClassLoadingHook hook){
        modifiers.add(hook);
    }
    
    public boolean removeModifier(IClassLoadingHook hook){
        return modifiers.remove(hook);
    }

}
