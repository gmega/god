/*
 * Created on Sep 7, 2005
 * 
 * file: ExtendedTableViewer.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import ddproto1.plugin.ui.launching.ICellEditorProvider;



public class ExtendedTableViewer extends StructuredViewer {
    
    private Table theTable;
    private ICellEditorProvider editorProvider;
    private TableEditor editor;
    
    private ICellEditorProvider.ICellEditor pending;
    
    public ExtendedTableViewer(Composite parent, int style){
        this.theTable = new Table(parent, style);
        this.editor = new TableEditor(theTable);
        editor.grabHorizontal = true;
        initTableEditing();
    }

    @Override
    protected Widget doFindInputItem(Object element) {
        if (equals(element, getRoot()))
            return theTable;
        return null;
    }

    @Override
    protected Widget doFindItem(Object element) {
        TableItem[] children = theTable.getItems();
        for (TableItem item : children) {
            Object data = item.getData();
            if (data != null && equals(data, element))
                return item;
        }

        return null;
    }

    @Override
    protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
        TableItem tItem = (TableItem)item; // Unchecked cast. Let's see if this gets me in trouble.
        if(fullMap){
            this.associate(element, tItem);
        }else{
            tItem.setData(element);
            /** Well, they say I shouldn't call this method, but I wouldn't know
             * how to update the StructuredViewer map without dissociating the 
             * old mapping otherwise.
             */
            super.mapElement(element, item);
        }
        
        /** Now update the widget following the model */
        int nColumns = theTable.getColumnCount();
        
        /** For now, I'm supporting only table label providers */
        ITableLabelProvider provider = (ITableLabelProvider)this.getLabelProvider();

        for(int i = 0; i < nColumns; i++){
            tItem.setText(i, provider.getColumnText(element, i));
            tItem.setImage(i, provider.getColumnImage(element, i));
        }
    }

    @Override
    protected List getSelectionFromWidget() {
        Widget [] items = theTable.getSelection();
        List<Object> contents = new ArrayList<Object>();
        for(Widget item : items)
            contents.add(item.getData());

        return contents;
    }

    protected void inputChanged(Object newInput, Object oldInput){
        this.refresh();
    }
    
    @Override
    protected void internalRefresh(Object element) {
        if(pending != null) this.deactivateEditor(pending);
        
        if (element == null || equals(element, getRoot())) {
            this.internalRefreshAll(getRoot(), true);
            return;
        }
        
        TableItem item = (TableItem)this.findItem(element);
        if(item != null)
            this.updateItem(item, element);
    }
    
    private void internalRefreshAll(Object element, boolean updateLabels){
        
        /** NOTE: This method is 99% pasted code from TableViewer. 
         * Very few modifications. I decided to paste the code because
         * it contains so many subtleties. 
         */
        
        Object[] children = getSortedChildren(getRoot());
        TableItem[] items = theTable.getItems();
        int min = Math.min(children.length, items.length);
        for (int i = 0; i < min; ++i) {

            TableItem item = items[i];
                
            // if the element is unchanged, update its label if appropriate
            if (equals(children[i], item.getData())) {
                if (updateLabels) {
                    updateItem(item, children[i]);
                } else {
                    // associate the new element, even if equal to the old
                    // one,
                    // to remove stale references (see bug 31314)
                    associate(children[i], item);
                }
            } else {
                // updateItem does an associate(...), which can mess up
                // the associations if the order of elements has changed.
                // E.g. (a, b) -> (b, a) first replaces a->0 with b->0, then
                // replaces b->1 with a->1, but this actually removes b->0.
                // So, if the object associated with this item has changed,
                // just disassociate it for now, and update it below.
                item.setText(""); //$NON-NLS-1$
                item.setImage(new Image[Math.max(1, theTable.getColumnCount())]);//Clear all images
                disassociate(item);
            }
        }
        // dispose of all items beyond the end of the current elements
        if (min < items.length) {
            for (int i = items.length; --i >= min;) {
                
                disassociate(items[i]);
            }
            theTable.remove(min, items.length - 1);
        }
        // Workaround for 1GDGN4Q: ITPUI:WIN2000 - TableViewer icons get
        // scrunched
        if (theTable.getItemCount() == 0) {
            theTable.removeAll();
        }
        // Update items which were removed above
        for (int i = 0; i < min; ++i) {
                            
            TableItem item = items[i];
            if (item.getData() == null) 
                updateItem(item, children[i]);
        }
        // add any remaining elements
        for (int i = min; i < children.length; ++i) {
            updateItem(new TableItem(theTable, SWT.NONE, i), children[i]);
        }

    }

    @Override
    public void reveal(Object element) {
        Widget wiii = this.doFindItem(element);
        /** Why do they test this? Isn't this supposed to be always true? */
        if(wiii instanceof TableItem)
            theTable.showItem((TableItem)wiii);
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {
        TableItem [] ti = new TableItem[l.size()];
        int j = 0;
        
        for(Object domainObject : l){
            TableItem item = (TableItem)this.findItem(domainObject);
            if(item == null || !(item instanceof TableItem)) continue;
            ti[j++] = item;
        }
        
        if(j < l.size()){
            TableItem [] newTi = new TableItem[j];
            System.arraycopy(ti, 0, newTi, 0, newTi.length);
            ti = newTi;
        }
               
        theTable.setSelection(ti);
        
        if(reveal)
            theTable.showItem(ti[0]);
    }
    
    private void initTableEditing(){
        
        this.getTable().addSelectionListener(new SelectionAdapter(){

            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(editorProvider == null ) return;
                
                if(pending != null) deactivateEditor(pending);
       
                // Identify the selected row
                TableItem item = (TableItem)e.item;
                if (item == null) return;

                final int column = 1;
              
                final String text = item.getText(column);
                final Object theObject = item.getData();
                
                ICellEditorProvider.IEditContext request = 
                    new ICellEditorProvider.IEditContext(){
                        public Table getTable() { return theTable; }
                        public Object getElement() { return theObject; }
                        public String getText() { return text; }
                        public int getColumn() { return column; }
                    };

                EditorCallback callback = new EditorCallback();
                
                /** Note that we set the callback after getting the cellEditor. The user should NOT
                 * attempt to use the callback by means other that through its listeners. 
                 */
                ICellEditorProvider.ICellEditor cellEditor = editorProvider.cellEditor(request,
                        callback);
                
                callback.setEditor(cellEditor, theObject);
                
                pending = cellEditor;
                editor.setEditor(cellEditor.getControl(), item, column);
            }
            
        });
    }

    @Override
    public Control getControl() {
        return theTable;
    }
    
    public Table getTable(){
        return theTable;
    }
    
    public void setCellEditorProvider(ICellEditorProvider provider){
        this.editorProvider = provider;
    }
    
    public ICellEditorProvider getCellEditorProvider(){
        return editorProvider;
    }
    
    private void deactivateEditor(ICellEditorProvider.ICellEditor editorData){
        Control theEditor = editorData.getControl();
        Assert.isTrue(theEditor.equals(editorData.getControl()));
        editorData.getCellEditorCallback().disappear();
        pending = null;
    }
    
    private class EditorCallback implements ICellEditorProvider.ICellHolderCallback{

        private ICellEditorProvider.ICellEditor editorData;
        private Object domain;
        
        public void setEditor(ICellEditorProvider.ICellEditor editor, Object domain){
            this.editorData = editor;
            this.domain = domain;
        }
        
        public void notifyEdit(int status) {
            deactivateEditor(editorData);
            if(status == ICellEditorProvider.STATUS_OK){
                updateItem(findItem(domain),domain);
            }
        }
    }
}
