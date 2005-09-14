/*
 * Created on Sep 12, 2005
 * 
 * file: DisplayWindow.java
 */
package ddproto1.primitiveGUI;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ddproto1.exception.IllegalStateException;
import ddproto1.interfaces.IMessageBox;

public class DisplayWindow extends ApplicationWindow implements IMessageBox{
    
    private List<IOpenListener> listeners = new LinkedList<IOpenListener>();
    
    private String title;
    private PrimitiveTextDisplay disp;
    private boolean isOpen = false;
    
    public DisplayWindow(String title){
        super(null);
        this.title = title;
    }
    
    @Override 
    protected Composite createContents(Composite parent){
        this.getShell().setText(title);
        this.getShell().setSize(600, 300);
        disp = new PrimitiveTextDisplay(parent, SWT.NONE);
        return parent;
    }

    public void openAsync(){
        
        Thread t = new Thread(new Runnable(){
            public void run() {
                open();
                Shell shell = getShell();
                Display display = shell.getDisplay();

                while (shell != null && !shell.isDisposed()) {
                    try {
                        if (!display.readAndDispatch())
                            display.sleep();
                    } catch (Throwable e) {
                       e.printStackTrace();
                    }
                }
                display.dispose();
            }
        });
        t.start();
    }
    
    @Override
    public int open(){
        setBlockOnOpen(false);
        int returnValue = super.open();
        isOpen = true;
        broadcastOpening();
        return returnValue;
    }
    
    private void broadcastOpening(){
        for(IOpenListener listener : listeners)
            listener.notifyOpening();
    }
    
    public void println(String s) {
        if(!isOpen) throw new IllegalStateException("Cannot print while window is still not open!");
        this.asyncPrint(s + "\n");
    }

    public void print(String s) {
        if(!isOpen) throw new IllegalStateException("Cannot print while window is still not open!");
        this.asyncPrint(s);
    }
    
    private void asyncPrint(String s){
        final String toPrint = s;
        
        this.getShell().getDisplay().asyncExec(new Runnable(){
            public void run(){
                disp.getText().append(toPrint);
            }
        });
    }
    
    public void addOpenListener(IOpenListener listener){
        listeners.add(listener);
    }
    
    public boolean removeOpenListener(IOpenListener listener){
        return listeners.remove(listener);
    }

}
