/*
 * Created on Sep 8, 2005
 * 
 * file: AttributeEditor.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import ddproto1.configurator.IAttribute;
import ddproto1.configurator.IObjectSpec;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.launching.IAttributeChangeListener;
import ddproto1.plugin.ui.launching.ICellEditorProvider;
import ddproto1.plugin.ui.launching.ICellEditorProvider.ICellEditor;
import ddproto1.plugin.ui.launching.ICellEditorProvider.ICellEditorCallback;


public class AttributeEditor implements ICellEditorProvider, ICellEditorCallback, ICellEditor{
    
    private static final int YES = 0;
    private static Logger logger = DDUIPlugin.getDefault().
                                        getLogManager().getLogger(AttributeEditor.class);
    
    private TextEditor theEditor;
    private ICellHolderCallback currentCallback;
    private IEditContext theRequest;
    private IObjectSpec spec;
    
    private TextEditor textEditor;
    private TextEditor comboEditor;
    
    private List<IAttributeChangeListener> listeners = new ArrayList<IAttributeChangeListener>();
    
    public ICellEditor cellEditor(IEditContext request, ICellHolderCallback cpc) {
       
        this.currentCallback = cpc;
        this.theRequest = request;
        
        IAttribute attribute = (IAttribute)request.getElement();
        if(attribute.acceptableValues() == IAttribute.ANY)
            theEditor = textEditorWith(request);
        else
            theEditor = comboEditorWith(request, attribute.acceptableValues());
        
        return this;
    }
    
    public TextEditor textEditorWith(IEditContext request){
        if(textEditor == null){
            final Table parentTable = request.getTable();
            
            textEditor = new TextEditor(){
                private Text text = new Text(parentTable, SWT.NONE);
                public String getText(){ return text.getText(); }
                public Control getControl(){ return text; }
            };
            
            this.addListenerTo(textEditor);
        }
        
        Text t = (Text)textEditor.getControl();
        t.setText(request.getText());
        
        return textEditor;
    }
  
    public TextEditor comboEditorWith(IEditContext request, Set<String> values){
        if(comboEditor == null){
            final Table parentTable = request.getTable();
            
            comboEditor = new TextEditor(){
                private Combo combo = new Combo(parentTable, SWT.READ_ONLY);
                public String getText(){ return combo.getItem(combo.getSelectionIndex()); }
                public Control getControl(){ return combo; }
            };
            
            this.addListenerTo(comboEditor);
        }
        
        Combo combo = (Combo)comboEditor.getControl();
        combo.removeAll();
        for(String value : values) combo.add(value);
        int selectionIndex = combo.indexOf(request.getText());
        if(selectionIndex != -1)
            combo.select(selectionIndex);
        else
            combo.select(0);
        
        return comboEditor;
    }
    
    public void addListenerTo(TextEditor te){
        
        Control control = te.getControl();
        
        control.addFocusListener(new FocusAdapter(){
            
            public void focusLost(FocusEvent fe) {
                IAttribute attrib = (IAttribute) theRequest.getElement();
                String text = theEditor.getText();
                String oldText = null;
                
                try{
                    oldText = spec.getAttribute(attrib.attributeKey());
                }catch(UninitializedAttributeException ex){
                }catch(IllegalAttributeException ex){ 
                    logger.error("Reported attribute doesn't exist (concurrent modification?) ", ex);
                }
                
                /** Nothing to do if old text is equal to the new one. */ 
                if(oldText != null && text.equals(oldText)){
                    return;
                }
                
                int modify = YES;
                try {
                    try{
                        if (spec.isContextAttribute(attrib.attributeKey())) {
                            MessageDialog dialog = new MessageDialog(
                                    theRequest.getTable().getShell(),
                                    "Attribute Modification Warning",
                                    null,
                                    "Warning: this attribute is marked as being extracted "
                                        + " from its context. That means that editing it will almost certainly "
                                        + "result in disaster unless you really know what you're doing. Are you "
                                        + "sure you want to proceed?",
                                        MessageDialog.WARNING, new String[] { "yes",
                                            "no" }, 1);

                            modify = dialog.open();
                        }
                    }catch(UninitializedAttributeException ex) { /** This is OK. */ }
                    
                    if (modify == YES){
                        spec.setAttribute(attrib.attributeKey(), text);
                        broadcast(text);
                    }
                }catch (AttributeAccessException ex) {
                    logger.error("Edit failed.", ex);
                    currentCallback.notifyEdit(ICellEditorProvider.STATUS_ABORT);
                    return;
                }finally{
                    oldText = null;
                }
                
                currentCallback.notifyEdit(ICellEditorProvider.STATUS_OK);
                broadcast(theEditor.getText());
            }
        });

    }
    
    private void broadcast(String newText){
        for(IAttributeChangeListener listener : listeners){
            listener.attributeChanged((IAttribute)theRequest.getElement(), newText);
        }
    }
    
    public void addAttributeChangeListener(IAttributeChangeListener listener){
        listeners.add(listener);
    }
    
    public boolean removeAttributeChangeListener(IAttributeChangeListener listener){
        return listeners.remove(listener);
    }

    
    public void disappear() {
        theEditor.getControl().setVisible(false);
    }
    
    public void setSpecUnderEdit(IObjectSpec spec){
        this.spec = spec;
    }
    
    public Control getControl() {
        return theEditor.getControl();
    }

    public ICellEditorCallback getCellEditorCallback() {
        return this;
    }

    private interface TextEditor{
        public String getText();
        public Control getControl();
    }

}
