/*
 * Created on Jun 5, 2006
 * 
 * file: DistributedTester.java
 */
package ddproto1.debugger.managing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.SpecLoader;
import ddproto1.configurator.XMLConfigurationParser;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.plugin.INodeList;
import ddproto1.debugger.managing.ILocalNodeManager;
import ddproto1.debugger.managing.INodeManagerFactory;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.identification.IGUIDManager;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.launcher.IApplicationLauncher;
import ddproto1.launcher.procserver.IProcessEventListener;
import ddproto1.launcher.procserver.ProcessEventAdapter;
import ddproto1.util.DelayedResult;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.IUndoAction;
import ddproto1.util.collection.UndoList;

public class LaunchHelper implements IConfigurationConstants{
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(LaunchHelper.class);
    
    public LaunchHelper() {}
    
    public void loadDistributedSystemSpec(URL configFile, Properties vars, SpecLoader loader) 
        throws Exception{
        XMLConfigurationParser parser = new XMLConfigurationParser(loader);
        
        /** Acquires a reference to the root spec. */
        IObjectSpec rootSpec = parser.parseConfig(configFile, vars);
        IObjectSpec nodeList = rootSpec.getChildOfType(NODE_LIST);
        try{
            synchronized(this){
                applyRootSpecAttributes(rootSpec);
                transferChildren(nodeList);
            }
        }catch(Exception ex){
            logger.error("Error while applying properties to global agent.", ex);
            throw ex; 
        }
    }
    
    private void applyRootSpecAttributes(IObjectSpec rootSpec)
        throws AttributeAccessException
    {
        Preferences prefs = 
            GODBasePlugin.getDefault().getPluginPreferences();
        for(String key : rootSpec.getAttributeKeys()){
            prefs.setValue(key, rootSpec.getAttribute(key));
        }
    }
    
    private void transferChildren(IObjectSpec rootSpec)
        throws AttributeAccessException, DuplicateSymbolException
    {
        INodeList nl = GODBasePlugin.getDefault().getConfigurationManager().getNodelist();
        for(IObjectSpec nodeSpec : rootSpec.getChildrenOfType(NODE)){
            nl.rebindSpec(nodeSpec);
        }
    }

    
    public ILocalNodeManager launchApplication(final IObjectSpec nodeSpec, ILaunch launch)
    		throws LauncherException
    {
        final IGUIDManager dgManager = GODBasePlugin.getDefault().debuggeeGUIDManager();
        
        //TODO Rollback code hasn't been tested. Write a test that exercises at least a part of it.
        UndoList<IUndoAction> uList = new UndoList<IUndoAction>();
        
        try{
            INodeManagerFactory factory = this.getFactory(nodeSpec.getType().getConcreteType());
            
            /** Begins by leasing a GUID to this node manager. */
            String intendedGuid = nodeSpec.getAttribute(GUID_ATTRIBUTE);
            final IUndoAction leaseGUID = new IUndoAction(){
                public void undo() throws Exception {
                    dgManager.releaseGUID(nodeSpec);
                }
            };
            uList.registerForRollback(leaseGUID, leaseGUID);
            
            if(intendedGuid.equals(AUTO))
                dgManager.leaseGUID(nodeSpec);
            else
                dgManager.leaseGUID(nodeSpec, new Integer(intendedGuid));

            /** Now the ID has been leased, we can instantiate the node 
             * manager.
             */
            final ILocalNodeManager nManager = factory.createNodeManager(nodeSpec);
            IUndoAction createNodeManager = new IUndoAction(){
                public void undo() throws Exception {
                    // Don't know how to undo this in a decent way. 
                }
            };
            uList.registerForRollback(createNodeManager, createNodeManager);
            
            /** 
             * Signals the node proxy that it should prepare to 
             * receive a remote connection. This remote connection 
             * may be attempted during launch, and it should block
             * until client calls connect on the node proxy.
             * 
             * We don't need to register this for rollback, because the 
             * createNodeManager undo action will already rollback it.
             */
            nManager.prepareForConnecting();  
                        
            IServiceLocator locator = 
                (IServiceLocator)Lookup.serviceRegistry().locate(IConfigurationConstants.SERVICE_LOCATOR);
            IObjectSpec launcherSpec = nodeSpec.getChildSupporting(IApplicationLauncher.class);
            IApplicationLauncher l = (IApplicationLauncher)locator.incarnate(launcherSpec);
            
            List <IProcessEventListener> listeners =
                new ArrayList<IProcessEventListener>();
            
            /** When the process dies, we... */
            listeners.add(new ProcessEventAdapter(){
                @Override
                public void notifyProcessKilled(int exitValue) {
                    /** Release it's GUID. */
                    try{
                        leaseGUID.undo();
                    }catch(Exception ex) { 
                        logger.error("Failed to release GUID.");
                    }
                }
            });

            /** We now launch the process. No need to rollback as well,
             * since the other actions we registered earlier will take 
             * care of it. */
            final IProcess process = l.launchOn(launch, listeners);
            nManager.setProcess(process);
            launch.addProcess(process);
            launch.addDebugTarget((IDebugTarget)nManager.getAdapter(IDebugTarget.class));
                        
            /** nManager is ready to connect. */
            return nManager;
        }catch(Throwable ex){
            uList.rollback(logger);
            throw new LauncherException("Unable to launch.", ex);
        }	
    }
   
    public ILocalNodeManager launchApplication(String name, ILaunch launch) 
        throws LauncherException{
        
        IObjectSpec nodeSpec = GODBasePlugin.getDefault()
                .getConfigurationManager().getNodelist().getSpec(name);
        if(nodeSpec == null)
            throw new LauncherException("Can't find node with name " + name);
        
        return launchApplication(nodeSpec, launch);
    }
    
    public void killApplication(String name){ }

    public void loadLaunchConstraintSpec(File f) 
        throws IOException, ParseException { }
    
    public void runConstraintSpec(String rootName) { }
    
    private INodeManagerFactory getFactory(String nodetype)
        throws NoSuchSymbolException
    {
        // TODO This is hardcoded. Substitute with flexible approach.
        if(nodetype.equals("ddproto1.debugger.managing.VirtualMachineManager"))
            return VMManagerFactory.getInstance();
        else
            throw new NoSuchSymbolException("Unknown node type " + nodetype);
    }
}
