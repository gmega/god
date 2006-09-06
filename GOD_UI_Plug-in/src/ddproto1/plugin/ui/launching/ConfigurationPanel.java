 package ddproto1.plugin.ui.launching;

import java.io.IOException;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import ddproto1.configurator.IObjectSpec;
import ddproto1.plugin.ui.launching.providers.ExtendedTableViewer;
import ddproto1.plugin.ui.launching.providers.ObjectSpecContentAdapter;

public class ConfigurationPanel extends Composite {

    private TreeViewer configurationTree = null;
    private ExtendedTableViewer propertySheet = null;
    private ListViewer allowedChildrenList = null;
    private ComboViewer availableImpls = null;

    private Group group = null;
    private Group group1 = null;
    private Group group2 = null;
    private Group group3 = null;
    
    private Button addButton = null;
    private Button removeButton = null;
    private Button switchImplButton = null;
    
    private Label label1 = null;
    private Label label2 = null;
    
    ConfigurationPanelMediator mediator;
    
    private IObjectSpec rootSpec;
    
    private SashForm sash;
    
    public ConfigurationPanel(Composite parent, int style) {
        super(parent, style);
        initialize();
    }

    private void initialize() {
        setSize(new org.eclipse.swt.graphics.Point(657,306));
        sash = new SashForm(this, SWT.HORIZONTAL);
        this.setLayout(new FillLayout());
        createGroup2();
        createGroup();
        createGroup1();
        createMediators();
        sash.pack();
    }
    
    private void createMediators() {
        try{
            mediator = new ConfigurationPanelMediator(
                    configurationTree, propertySheet, allowedChildrenList, availableImpls,
                    addButton, removeButton, this);

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * This method initializes tree	
     *
     */
    private void createTree() {
        configurationTree = new TreeViewer(group, SWT.NONE);
        configurationTree.getTree().setBounds(new org.eclipse.swt.graphics.Rectangle(5,15,246,281));
    }
    
    public void setObjectSpecRoot(IObjectSpec root) throws IOException{
        configurationTree.setInput(root); // Got to do this first.
        mediator.addForbidden(root);
        /** Ugly hack: Set a virtual input so that the TreeViewer displays
         * the root element.
         */
        configurationTree.setInput(ObjectSpecContentAdapter.virtualRoot);
        rootSpec = root;
    }
    
    public IObjectSpec getObjectSpecRoot(){
        return rootSpec;
    }

    /**
     * This method initializes table	
     *
     */
    private void createTable() {
        propertySheet = new ExtendedTableViewer(group1, SWT.NONE);

        TableLayout layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(80, true));
        layout.addColumnData(new ColumnWeightData(80, true));
        layout.addColumnData(new ColumnWeightData(80, true));
        propertySheet.getTable().setLayout(layout);
        
        TableColumn properties = new TableColumn(propertySheet.getTable(),
                SWT.LEFT);
        properties.setText("Property");
        TableColumn value = new TableColumn(propertySheet.getTable(), SWT.LEFT);
        value.setText("Value");
        
        propertySheet.getTable().setHeaderVisible(true);
        propertySheet.getTable().setLinesVisible(true);
        propertySheet.getTable().setBounds(
                new org.eclipse.swt.graphics.Rectangle(5, 15, 171, 281));
        
    }

    /**
     * This method initializes group	
     *
     */
    private void createGroup() {
        group = new Group(sash, SWT.NONE);
        group.setText("Configuration");
        group.setLayout(new FillLayout());
        createTree();
    }

    /**
     * This method initializes group1	
     *
     */
    private void createGroup1() {
        group1 = new Group(sash, SWT.NONE);
        group1.setText("Properties");
        group1.setLayout(new FillLayout());
        createTable();
    }

    /**
     * This method initializes group2	
     *
     */
    private void createGroup2() {
        group2 = new Group(sash, SWT.NONE);
        group2.setText("Children");

        group2.setLayout(new GridLayout());
        label2 = new Label(group2, SWT.NONE);
        label2.setText("Required types");

        allowedChildrenList = new ListViewer(group2, SWT.V_SCROLL);
        GridData listData = new GridData(GridData.FILL_BOTH);
        listData.horizontalSpan = 14;
        allowedChildrenList.getList().setLayoutData(listData);

        label1 = new Label(group2, SWT.NONE);
        label1.setText("Available implementations");
        createCombo1();
        
        createGroup3();
    }

    /**
     * This method initializes combo1	
     *
     */
    private void createCombo1() {
        availableImpls = new ComboViewer(group2, SWT.READ_ONLY);
        availableImpls.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    /**
     * This method initializes group3	
     *
     */
    private void createGroup3() {
        group3 = new Group(group2, SWT.SHADOW_ETCHED_IN);
        group3.setLayout(new GridLayout(2, true));
        group3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        addButton = new Button(group3, SWT.NONE);
        addButton.setText("Add");
        GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, true, true);
        gd.widthHint = 75;
        addButton.setLayoutData(gd);
                
        removeButton = new Button(group3, SWT.NONE);
        removeButton.setText("Remove");
        GridData gd2 = new GridData(SWT.END, SWT.CENTER, true, true);
        gd2.widthHint = 75;
        removeButton.setLayoutData(gd2);
    }
    
    public void addAttributeChangeListener(IAttributeChangeListener listener){
        mediator.addAttributeChangeListener(listener);
    }
    
    public void removeChangeListener(IAttributeChangeListener listener){
        mediator.removeAttributeChangeListener(listener);
    }

}  //  @jve:decl-index=0:visual-constraint="12,10"
