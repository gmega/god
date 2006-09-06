/*
 * Created on Jun 26, 2006
 * 
 * file: ProcessServerLauncher.java
 */
package ddproto1.launcher;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.ILaunch;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import ddproto1.debugger.managing.identification.IGUIDManager;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.procserver.IProcessEventListener;
import ddproto1.launcher.procserver.IProcessServerManager;
import ddproto1.launcher.procserver.IRemoteCommandExecutor;
import ddproto1.launcher.procserver.ProcessEventAdapter;
import ddproto1.launcher.procserver.PyExecCommandLine;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;

public class ProcessServerLauncher implements IApplicationLauncher, ProcessServerConstants, IConfigurationConstants {

    private static final Logger logger = MessageHandler.getInstance().getLogger(ProcessServerLauncher.class);
    
    private Map<String, String> attributeMap = new HashMap<String, String>();
    
    public String getAttribute(String key) throws IllegalAttributeException,
            UninitializedAttributeException {
        if(!attributeMap.containsKey(key))
            throw new IllegalAttributeException();
        return attributeMap.get(key);
    }

    public void setAttribute(String key, String val)
            throws IllegalAttributeException, InvalidAttributeValueException {
        attributeMap.put(key, val);
    }

    public boolean isWritable() {
        return true;
    }

    public RemoteGODProcess launchOn(ILaunch launch, List <IProcessEventListener> listeners)
        throws ConfigException, IncarnationException, LauncherException {

        /** Starts by trying to render the commandline and acquiring a reference
         * to the remote executor. */
        String [] commandLine;
        String localAddress;
        ICommandLine pyExec;
        IRemoteCommandExecutor rcomExe;
        
        IObjectSpec _this; 
        
        try{
            localAddress = getAttribute(LOCAL_AGENT_ADDRESS);
            _this = getServiceLocator().getMetaobject(this);
            commandLine = getCommandLine(_this).renderCommandLine();
            rcomExe = getExecutor(_this);
            pyExec = new PyExecCommandLine(rcomExe.getCommandLine());
            setSame(CALLBACK_OBJECT_PATH, pyExec);
            setSame(RMI_REGISTRY_PORT, pyExec);
            setSame(LOG4JCONFIG, pyExec);
            setSame(GLOBAL_AGENT_ADDRESS, pyExec);
            pyExec.setAttribute(TRANSPORT_PROTOCOL, TCP);
            pyExec.setAttribute(LR_INSTANTIATION_POLICY, SHOULD_USE_EXISTING);
            rcomExe.setCommandLine(pyExec);
        }catch(NoSuchSymbolException ex){
            throw new ConfigException(ex);
        }catch(AmbiguousSymbolException ex){
            throw new ConfigException(ex);
        }catch(AttributeAccessException ex){
            throw new ConfigException(ex);
        }
        
        /** Now we get/generate a label for the process. */
        String processLabel = getAttribute(this, IConfigurationConstants.NAME_ATTRIB, null);
        if(processLabel == null){
            processLabel = "Remote process at [" + localAddress + "]";
        }
        
        /** Attempts to start the process server manager,
         * if not already started.
         */
        IProcessServerManager psManager = 
            GODBasePlugin.getDefault().getProcessServerManager();
        try{
            psManager.startIfPossible();
        }catch(Exception ex){
            throw new LauncherException("Error while attempting to start the Process Server Manager.", ex);
        }
        
        /** Finally, handles the ID lease. */
        final GODBasePlugin plugin = GODBasePlugin.getDefault();
        String preassigned = getAttribute(this, PROC_HANDLE_ATTRIBUTE, null);
        
        /** "Ticket" for manipulating the process handle. */
        final Object handleOwner = new Object();
        
        Integer pHandle = null;
        IProcessServer remoteServer = null;
       
        try{
            if(preassigned != null && !preassigned.equals(IConfigurationConstants.AUTO)){
                pHandle = new Integer(preassigned);
                plugin.processGUIDManager().leaseGUID(handleOwner, pHandle);
            }else{
                pHandle = plugin.processGUIDManager().leaseGUID(handleOwner);
            }
            /** We're done with configuration.
             * Attempts to acquire a reference to the remote process server. */
            remoteServer = psManager.getProcessServerFor(_this, rcomExe);
        }catch(Throwable t){
            if(pHandle != null)
                plugin.processGUIDManager().releaseGUID(handleOwner);
            try{
                if(remoteServer != null)
                    remoteServer.shutdownServer(true);
            }catch(RemoteException ex){
                logger.error("Failed to shutdown remote server. You might have to login and kill it manually.", ex);
            }
            throw new LauncherException("Error while leasing GUID or starting server.", t);
        }
        
        /** We create a listener to auto-release the GUID when
         * the process dies. */
        IProcessEventListener idReturner =
            new ProcessEventAdapter(){
                @Override
                public void notifyProcessKilled(int exitValue) {
                    plugin.processGUIDManager().releaseGUID(handleOwner);
                }
        };

        LaunchParametersDTO lpDto = new LaunchParametersDTO(commandLine,
                new LaunchParametersDTO.EnvironmentVariable [] {}, pHandle);
        
        psManager.registerProcessListener(idReturner, pHandle);
        RemoteGODProcess rgProcess = new RemoteGODProcess(processLabel, launch);
        psManager.registerProcessListener(rgProcess, pHandle);
                
        try{
            /** We now register the remaining listeners */
            for(IProcessEventListener listener : listeners)
                psManager.registerProcessListener(listener, pHandle);

            /** Finally, we try to launch the process. */
            IRemoteProcess rProcess = remoteServer.launch(lpDto);
            rgProcess.setProcess(rProcess);

            return rgProcess;
        }catch(Throwable t){
            psManager.removeProcessListener(idReturner, pHandle);
            psManager.removeProcessListener(rgProcess, pHandle);
            for(IProcessEventListener listener : listeners)
                psManager.removeProcessListener(listener, pHandle);
            plugin.processGUIDManager().releaseGUID(handleOwner);
            
            throw new LauncherException("Launch has failed.", t);
        }
    }
    
    private void setSame(String id, IConfigurable configurable)
        throws AttributeAccessException
    {
        configurable.setAttribute(id, getAttribute(id));
    }
    
    private ICommandLine getCommandLine(IObjectSpec _this) 
        throws AmbiguousSymbolException, IncarnationException, NoSuchSymbolException{
        IObjectSpec cLine = _this.getChildSupporting(ICommandLine.class);
        return (ICommandLine)getServiceLocator().incarnate(cLine);
    }
    
    private IRemoteCommandExecutor getExecutor(IObjectSpec _this)
        throws AmbiguousSymbolException, IncarnationException, NoSuchSymbolException{
        IObjectSpec cLine = _this.getChildSupporting(IRemoteCommandExecutor.class);
        return (IRemoteCommandExecutor)getServiceLocator().incarnate(cLine);
    }
    
    private IServiceLocator getServiceLocator() throws NoSuchSymbolException{
        return (IServiceLocator)Lookup.serviceRegistry().locate(IConfigurationConstants.SERVICE_LOCATOR);
    }

    private String getAttribute(IConfigurable conf, String key, String defaultValue){
        try{
            return conf.getAttribute(key);
        }catch(AttributeAccessException ex){
            return defaultValue;
        }
    }
}
