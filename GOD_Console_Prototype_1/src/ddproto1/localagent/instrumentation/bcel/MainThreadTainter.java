/*
 * Created on Sep 17, 2005
 * 
 * file: MainThreadTainter.java
 */
package ddproto1.localagent.instrumentation.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.log4j.Logger;

import ddproto1.localagent.Tagger;

public class MainThreadTainter implements IClassLoadingHook{
    
    private static final Logger logger = Logger.getLogger(MainThreadTainter.class);
    
    private BCELClientSideTransformer transformer;
    
    public MainThreadTainter(BCELClientSideTransformer transformer){
        this.transformer = transformer;
    }
    
    public JavaClass modifyClass(JavaClass jc, ClassLoader loader) {
        Tagger tagger = Tagger.getInstance();
        tagger.tagCurrent();
        logger.debug("Marked thread with id " + tagger.currentTag());
        /** Remove itself from the transformation list. */ 
        transformer.asyncRemoveModifier(this);
        return jc;
    }

}
