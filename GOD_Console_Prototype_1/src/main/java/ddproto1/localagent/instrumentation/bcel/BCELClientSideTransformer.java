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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.log4j.Logger;


public class BCELClientSideTransformer implements ClassFileTransformer{

    private Set<IClassLoadingHook> toRemove = new HashSet<IClassLoadingHook>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private List<IClassLoadingHook> modifiers = new ArrayList<IClassLoadingHook>();
    Logger pLogger = Logger.getLogger(BCELClientSideTransformer.class);
            
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try{
            if(pLogger.isDebugEnabled()) pLogger.debug("Parsing " + className);
            /** Creates a BCEL class file and delegates processing to its hooks. 
             * I can't really assert the name of the source file for this class, so I pass
             * the "garbage" string. I think BCEL doesn't use it, but if it does then I better 
             * maximize the chances of provoking some grotesque error that can be caught. 
             * Its better than having some bizarre behaviour and not knowing why. */
            ClassParser cp = new ClassParser(new ByteArrayInputStream(classfileBuffer), "garbage");
            JavaClass clazz = cp.parse();
            
            /** Tries to keep synchronization overhead low. 
             * We use a read-write lock to avoid removal of elements 
             * from the transformation hook list while we're reading it. */
            if(!toRemove.isEmpty()){
                Lock wl = lock.writeLock();
                wl.lock();
                this.removePending();
                wl.unlock();
            }
            
            Lock rl = lock.readLock();
            rl.lock();
            for(IClassLoadingHook modifier : modifiers){
                clazz = modifier.modifyClass(clazz, loader);
            }
            rl.unlock();
            
            return clazz.getBytes();
        }catch(Throwable t){
            pLogger.error("Failed instrumenting class " + className, t);
            return classfileBuffer;
        }
    }
    
    public void asyncRemoveModifier(IClassLoadingHook hook){
        toRemove.add(hook);
    }
    
    private void removePending(){
        for(Iterator<IClassLoadingHook> it = toRemove.iterator(); it.hasNext();){
            IClassLoadingHook removedHook = it.next();
            it.remove();
            modifiers.remove(removedHook);
        }
    }
        
    public void addModifier(IClassLoadingHook hook){
        modifiers.add(hook);
    }
    
    public boolean removeModifier(IClassLoadingHook hook){
        return modifiers.remove(hook);
    }

}
