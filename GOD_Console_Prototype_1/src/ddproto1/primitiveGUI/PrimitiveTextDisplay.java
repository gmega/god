/*
 * Created on Sep 12, 2005
 * 
 * file: PrimitiveTextDisplay.java
 */
package ddproto1.primitiveGUI;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;

public class PrimitiveTextDisplay extends Composite {

    private Text text = null;

    public PrimitiveTextDisplay(Composite parent, int style) {
        super(parent, style);
        initialize();
    }

    private void initialize() {
        this.setLayout(new FillLayout());
        text = new Text(this, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setEditable(false);
        this.setLayout(new FillLayout());
    }
    
    public Text getText(){
        return text;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
