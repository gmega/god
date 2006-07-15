/*
 * Created on Sep 6, 2005
 * 
 * file: ICellEditorProvider.java
 */
package ddproto1.plugin.ui.launching;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

public interface ICellEditorProvider {
    
    public int STATUS_OK = 0;
    public int STATUS_ABORT = -1;
    
    public ICellEditor cellEditor(IEditContext request, ICellHolderCallback cpc);

    /**
     * This interface allows access to all aspects of the current cell editor
     * request. It should be used by the cell editor provider as context information 
     * for creating the apropriate control.
     * 
     * @author giuliano
     *
     */
    interface IEditContext{
        public Table getTable();    // Table under edit
        public Object getElement(); // Associated element
        public String getText();    // Text in table
        public int getColumn();     // Column being edited
    }
    
    /**
     * Allows access to the editor's control and to it's disposal callback. 
     * 
     * @author giuliano
     *
     */
    interface ICellEditor{
        public Control getControl();
        public ICellEditorCallback getCellEditorCallback(); 
    }
    
    /**
     * Interface that groups the messages sent by the cell editor to
     * the provider's user. 
     * 
     * @author giuliano
     *
     */
    interface ICellHolderCallback{
        /**
         * The cell editor should call this method once the editing process is
         * complete. 
         * 
         * @param status
         */
        public void notifyEdit(int status);
    }
    
    /**
     * Interface that groups the messages sent by the provider's user
     * to the cell editor. The provider is responsible for adapting the
     * message to a suitable request to the editor. 
     * 
     * @author giuliano
     *
     */
    interface ICellEditorCallback{
        /**
         * Tells the cell editor that it should dispose of itself and stop accepting
         * edits. 
         */
        public void disappear();
    }
}
