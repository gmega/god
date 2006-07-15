/*
 * Created on Sep 5, 2005
 * 
 * file: AvailableImplementationContentProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import ddproto1.configurator.IObjectSpecType;
import ddproto1.plugin.ui.launching.IImplementationScanner;
import ddproto1.plugin.ui.launching.IImplementationScannerListener;


public class AvailableImplementationContentProvider implements
        IStructuredContentProvider, IImplementationScannerListener {

    private IImplementationScanner scanner;
    private Iterable<IObjectSpecType> theData = null;
    private Display display;
    private ComboViewer viewer;
    
    public AvailableImplementationContentProvider(IImplementationScanner scanner, ComboViewer viewer){
        this.scanner = scanner;
        this.scanner.addAnswerListener(this);
        this.viewer = viewer;
        this.display = viewer.getControl().getDisplay();
    }
    
    public Object[] getElements(Object inputElement) {
        
        if(inputElement == null) return null;
        synchronized(this){
            if(theData == null){
                String type = (String)inputElement;
                scanner.asyncRetrieveImplementationsOf(type);
                return new Object[]{ };
            }
        
            List<IObjectSpecType> list = new ArrayList<IObjectSpecType>();
            for(IObjectSpecType data : theData)
                list.add(data);

            return list.toArray();
        }
    }

    public void dispose() { }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        theData = null;
    }

    public synchronized void receiveAnswer(Iterable<IObjectSpecType> answerList) {
        this.theData = answerList;
        display.asyncExec(new Runnable(){
            public void run() {
                viewer.refresh();
                viewer.getCombo().select(0);
            }
        });
    }
}
