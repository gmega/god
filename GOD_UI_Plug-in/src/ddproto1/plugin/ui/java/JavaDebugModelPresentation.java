/*
 * Created on 8/09/2006
 * 
 * file: JavaDebugModelPresentation.java
 */
package ddproto1.plugin.ui.java;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.ui.IEditorInput;

import ddproto1.plugin.ui.GODDebugModelPresentation;

/**
 * Simple debug model presentation for the supplied Java Debugger.
 * It delegates all the hard stuff to JDT internal classes.
 * 
 * @author giuliano
 *
 */
public class JavaDebugModelPresentation extends GODDebugModelPresentation{
    
    private IDebugModelPresentation fJDTModelPresentation;
    
    @Override
    public IEditorInput getEditorInput(Object element) {
        return getJDTDebugModelPresentation().getEditorInput(element);
    }

    @Override
    public String getEditorId(IEditorInput input, Object element) {
       return getJDTDebugModelPresentation().getEditorId(input, element);
    }
    
    public void dispose(){
        super.dispose();
        IDebugModelPresentation localCopy = null;
        synchronized(this){
            localCopy = fJDTModelPresentation;
            fJDTModelPresentation = null;
        }
        
        if(localCopy != null)
            localCopy.dispose();
    }
    
    private synchronized IDebugModelPresentation getJDTDebugModelPresentation(){
        if(fJDTModelPresentation == null){
            fJDTModelPresentation = 
                DebugUITools.newDebugModelPresentation(JDIDebugPlugin.getUniqueIdentifier());
        }
        return fJDTModelPresentation;
    }

}
