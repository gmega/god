/*
 * Created on Sep 3, 2005
 * 
 * file: ConfigurationPanelMediator.java
 */
package ddproto1.plugin.ui.launching;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IAttribute;
import ddproto1.configurator.IContextSearchable;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.plugin.ui.DDUIPlugin;
import ddproto1.plugin.ui.launching.providers.AttributeEditor;
import ddproto1.plugin.ui.launching.providers.AvailableImplementationContentProvider;
import ddproto1.plugin.ui.launching.providers.AvailableImplementationsLabelProvider;
import ddproto1.plugin.ui.launching.providers.ExtendedTableViewer;
import ddproto1.plugin.ui.launching.providers.MissingChildrenContentProvider;
import ddproto1.plugin.ui.launching.providers.MissingChildrenLabelProvider;
import ddproto1.plugin.ui.launching.providers.ObjectSpecContentAdapter;
import ddproto1.plugin.ui.launching.providers.ObjectSpecLabelAdapter;
import ddproto1.plugin.ui.launching.providers.ObjectSpecTableContentProvider;
import ddproto1.plugin.ui.launching.providers.ObjectSpecTableLabelProvider;

public class ConfigurationPanelMediator implements IAttributeChangeListener {
    
    private static final Logger logger = 
        DDUIPlugin.getDefault().getLogManager().getLogger(ConfigurationPanelMediator.class);
    
    private TreeViewer tree;
    private ExtendedTableViewer table;
    private ListViewer list;
    private ComboViewer implList;
    private Button addButton;
    private Button removeButton;
    private AttributeEditor editor;
    
    private Composite parent;
    
    private Set<IObjectSpec> forbiddenSpecs = new HashSet<IObjectSpec>();
    
    public ConfigurationPanelMediator(TreeViewer _tree, ExtendedTableViewer _table,
            ListViewer _list, ComboViewer _implList, Button _addButton, 
            Button _removeButton, Composite _parent)
        throws IOException
    {
        this.tree = _tree;
        this.table = _table;
        this.list = _list;
        this.removeButton = _removeButton;
        this.parent = _parent;
        this.implList = _implList;
        _table.setUseHashlookup(true);
        this.addButton = _addButton;
        this.addButton.setEnabled(false);
        this.editor = new AttributeEditor();
        
        tree.setContentProvider(new ObjectSpecContentAdapter());
        tree.setLabelProvider(new ObjectSpecLabelAdapter());
        
        ObjectSpecTableContentProvider tableContentProvider = new ObjectSpecTableContentProvider();
        table.setContentProvider(tableContentProvider);
        table.setLabelProvider(new ObjectSpecTableLabelProvider(tableContentProvider));
        table.setCellEditorProvider(editor);
        editor.addAttributeChangeListener(this);
        
        MissingChildrenContentProvider listContentProvider = new MissingChildrenContentProvider();
        list.setContentProvider(listContentProvider);
        list.setLabelProvider(new MissingChildrenLabelProvider(listContentProvider));
        
        implList.setContentProvider(new AvailableImplementationContentProvider(
                GODBasePlugin.getDefault().getConfigurationManager()
                        .getImplementationScanner(), implList));
        implList.setLabelProvider(new AvailableImplementationsLabelProvider());
                
        tree.addSelectionChangedListener(new ISelectionChangedListener(){
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                IObjectSpec spec = (IObjectSpec)selection.getFirstElement();
                
                table.setInput(spec);
                list.setInput(spec);
                
                if(spec == null) return;
                
                try{
                    implList.setInput(spec.getType().getInterfaceType());
                }catch(AttributeAccessException ex){ 
                    logger.error("Error.", ex);
                }
                
                try{
                    implList.setSelection(new StructuredSelection(spec.getType().getConcreteType()));
                }catch(AttributeAccessException ex){ 
                    implList.getCombo().select(0);
                }

                editor.setSpecUnderEdit((IObjectSpec)spec);
            }
        });
        
        removeButton.addSelectionListener(new SelectionAdapter(){

            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = (IStructuredSelection)tree.getSelection();
                IContextSearchable spec = (IContextSearchable)selection.getFirstElement();
                if(spec == null) return;
                if(forbiddenSpecs.contains(spec)){
                    MessageDialog error = new MessageDialog(
                            parent.getShell(),
                            "Not allowed",
                            null,
                            "You cannot remove this specification from the list.",
                            MessageDialog.INFORMATION, new String[] { "OK" }, 0);
                    error.open();
                    return;
                }
                
                IObjectSpec lastSpec = null;
                for(IObjectSpec parentSpec : spec.getAllParents()){
                    try{
                        parentSpec.removeChild((IObjectSpec)spec);
                        lastSpec = parentSpec;
                    }catch(IllegalAttributeException ex){
                        ex.printStackTrace();
                    }
                }
                tree.setSelection(new StructuredSelection(lastSpec));
                tree.refresh();
            }

        });
        
        list.getList().addSelectionListener(new SelectionAdapter(){
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                System.out.println("Selected default");
            }

            public void widgetSelected(SelectionEvent e){
                IStructuredSelection selection = (IStructuredSelection)list.getSelection();
                String type = (String)selection.getFirstElement();
                implList.setInput(type);
                addButton.setEnabled(true);
            }
            
        });
        
        
        addButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
                IStructuredSelection typeSelection = (IStructuredSelection)implList.getSelection();
                IStructuredSelection parentSelection = (IStructuredSelection)tree.getSelection();
                IObjectSpec parentSpec = (IObjectSpec)parentSelection.getFirstElement();
                IObjectSpecType specType = (IObjectSpecType)typeSelection.getFirstElement();
                
                if(parentSpec == null || specType == null){
                    error("You must select an implementation before adding a child.");
                    return;
                }

                try{
                    parentSpec.addChild(specType.makeInstance());
                    tree.refresh();
                    list.setInput(list.getInput());
                    if(list.getList().getItemCount() != 0)
                        list.getList().select(0);
                    else
                        addButton.setEnabled(false);
                                        
                }catch(Exception ex){
                    error(ex.getClass() + ": " + ex.toString());
                    return;
                }
            }
        });
    }
    
    public void addAttributeChangeListener(IAttributeChangeListener listener){
        editor.addAttributeChangeListener(listener);
    }
    
    public void removeAttributeChangeListener(IAttributeChangeListener listener){
        editor.removeAttributeChangeListener(listener);
    }
    
    public void attributeChanged(IAttribute attribute, String value){
        /** Might have activated some optional children; */
        tree.setInput(tree.getInput());
        /** or some optional attribute; */
        table.setInput(table.getInput());
        /** which may change the accepted child-type list; */
        list.setInput(list.getInput());
        /** and the implementation list. */
        implList.setInput(null);
    }
    
    private void error(String s){
        MessageDialog error = new MessageDialog(
                parent.getShell(),
                "Error",
                null,
                s,
                MessageDialog.ERROR, new String[] { "OK" }, 0);
        error.open();
    }
    
    public void addForbidden(IObjectSpec forbidden){
        forbiddenSpecs.add(forbidden);
    }
}
