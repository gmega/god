/*
 * Created on 28/08/2006
 * 
 * file: GODDebugModelPresentation.java
 */
package ddproto1.plugin.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.IDistributedThread;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.VirtualStackframe;
import ddproto1.util.traits.commons.ConversionUtil;

public class GODDebugModelPresentation extends LabelProvider implements IDebugModelPresentation{

    private static final Logger logger = 
        DDUIPlugin.getDefault().getLogManager().getLogger(GODDebugModelPresentation.class);
    private static final ConversionUtil cUtil = 
        ConversionUtil.getInstance();
    
    private final Map<String, Object> fAttributes =
        new HashMap<String, Object>();
    
    private volatile Image fVFImage;
    private volatile Image fVFDamageImage;
    private volatile Image fDTImage;
    
    public GODDebugModelPresentation()
        
    {
        loadIcons();
    }
    
    private void loadIcons(){
        fVFImage = DDUIPlugin.getImageDescriptor(
                UIDebuggerConstants.VIRTUAL_STACKFRAME_ICON).createImage();
        fVFDamageImage = DDUIPlugin.getImageDescriptor(
                UIDebuggerConstants.VIRTUAL_STACKFRAME_DMG_ICON).createImage();
        fDTImage = null;
    }
    
    
    public void setAttribute(String attribute, Object value) {
        fAttributes.put(attribute, value);
    }

    public Image getImage(Object element) {
        if(element instanceof VirtualStackframe)
            return imageForVSF((VirtualStackframe)element);
        else if(element instanceof DistributedThread)
            return imageForDT((DistributedThread)element);
        return null;
    }
    
    private Image imageForDT(DistributedThread thread) {
        return fDTImage;
    }

    private Image imageForVSF(VirtualStackframe stackframe) {
        if(stackframe.isDamaged())
            return fVFDamageImage;
        else
            return fVFImage;
    }

    public String getText(Object element) {
        /** A good refactoring here would be "replace conditionals
         * by polymorphism. However, we'd then need to assemble the
         * object graph somehow, and I'm trying to avoid spending
         * too much time with configuration again. 
         */
        
        /** Labels for threads. */
        if(element instanceof IThread){
            if(element instanceof ILocalThread){
                return getLocalThreadText((ILocalThread)element, false);
            }else if(element instanceof IDistributedThread){
                return getDistributedThreadText((IDistributedThread)element);
            }
        }
        
        /** Labels for Stack frames */
        if(element instanceof IStackFrame){
            if(element instanceof VirtualStackframe){
                return getVirtualStackframeText((VirtualStackframe) element);
            }
            return getStackframeText((IStackFrame) element);
        }
        
        return null;
    }
    
    private String getLocalThreadText(ILocalThread thread, 
            boolean includeSourceNode){
        StringBuffer threadLabel = new StringBuffer();
        if(includeSourceNode){
            String sourceName = "<error getting source node>";
            try{
                sourceName = thread.getDebugTarget().getName();
            }catch(DebugException ex){
                logger.error("Error while getting source node from thread.", ex);
            }
            threadLabel.append(sourceName);
            threadLabel.append(": ");
        }
        threadLabel.append("Thread [");
        String tName = "error getting name";
        try{
            tName = thread.getName();
        }catch(DebugException ex){
            logger.error("Error while fetching thread name.", ex);
        }
        
        threadLabel.append(tName);
        Integer guid = thread.getGUID();
        if(guid != null){
            threadLabel.append("] - [gid: ");
            threadLabel.append(cUtil.uuid2Dotted(thread.getGUID()));

        }
        threadLabel.append("]");
        return threadLabel.toString();
    }
    
    private String getDistributedThreadText(IDistributedThread thread){
        StringBuffer threadLabel = new StringBuffer();
        threadLabel.append("Distributed Thread [");
        threadLabel.append(cUtil.uuid2Dotted(thread.getId()));
        threadLabel.append("]");
        return threadLabel.toString();
    }
    
    private String getVirtualStackframeText(VirtualStackframe vsf){
        return getLocalThreadText(vsf.getThreadReference(), true);        
    }
    
    private String getStackframeText(IStackFrame frame){
        String frameName = "<unable to fetch label>";
        try{
            frameName = frame.getName();
        }catch(DebugException ex){
            if(!meansStaleFrame(ex))
                logger.error("Failed to fetch information from stack frame", ex);
        }
        return frameName;
    }
    
    private boolean meansStaleFrame(DebugException ex){
        return ex.getStatus().getCode() == DebuggerConstants.LOCAL_THREAD_RESUMED;
    }

    public void computeDetail(IValue value, IValueDetailListener listener) {
        listener.detailComputed(value, null);
    }

    public void dispose() {
        fDTImage.dispose();
        fVFImage.dispose();
        fVFDamageImage.dispose();
    }
    
    public IEditorInput getEditorInput(Object element) {
        return null;
    }

    public String getEditorId(IEditorInput input, Object element) {
        return null;
    }

}
