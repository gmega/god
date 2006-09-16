/*
 * Created on 9/09/2006
 * 
 * file: DelegatingSourceLocator.java
 */
package ddproto1.sourcemapper.generic;

import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

import ddproto1.debugger.managing.tracker.LocalStackframeWrapper;

public class DelegatingSourceLocator implements ISourceLocator {

    public Object getSourceElement(IStackFrame stackFrame) {
        if(!(stackFrame instanceof LocalStackframeWrapper))
            return null;

        IStackFrame realFrame = ((LocalStackframeWrapper)stackFrame).getUnderlyingFrame();
        ISourceLocator delegate = realFrame.getLaunch().getSourceLocator();
        if(delegate == null) return null;
        return delegate.getSourceElement(realFrame);
    }

}
