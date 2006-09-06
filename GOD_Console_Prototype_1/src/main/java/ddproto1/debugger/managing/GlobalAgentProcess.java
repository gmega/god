/*
 * Created on 23/08/2006
 * 
 * file: GlobalAgentProcess.java
 */
package ddproto1.debugger.managing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import ddproto1.GODBasePlugin;
import ddproto1.launcher.procserver.IProcessServerManager;
import ddproto1.util.IServiceLifecycle;

/**
 * Sketch implementation of a virtual global aprotectedgent process, which acts
 * as a parent for all other processes. 
 * 
 * Clients are not intended to instantiate or subclass this class.
 * 
 * @author giuliano
 *
 */
public class GlobalAgentProcess extends PlatformObject implements IProcess{

    private ILaunch fLaunch;
    private String fLabel;
    private IProcessServerManager fProcserverManager;
    private final Map<String, String> fAttributes = 
        Collections.synchronizedMap(new HashMap<String, String>());
    
    /**
     * Constructor for GlobalAgentProcess. 
     * 
     * @param launch
     * @param label
     * @param ProcserverManager
     */
    public GlobalAgentProcess(ILaunch launch, String label,
            IProcessServerManager ProcserverManager){
        setLaunch(launch);
        setLabel(label);
        setServer(ProcserverManager);
    }
    
    public synchronized String getLabel() {
        return fLabel;
    }
    
    public synchronized void setLabel(String label){
        fLabel = label;
    }

    public synchronized void setLaunch(ILaunch launch){
        fLaunch = launch;
    }
    
    public synchronized ILaunch getLaunch() {
        return fLaunch;
    }

    public IStreamsProxy getStreamsProxy() {
        return null;
    }

    public void setAttribute(String key, String value) {
        fAttributes.put(key, value);
    }

    public String getAttribute(String key) {
        return fAttributes.get(key);
    }

    /**
     * Kind of meaningless in this context.
     */
    public int getExitValue() throws DebugException {
        return 0;
    }

    public boolean canTerminate() {
        return getServer().currentState() == IServiceLifecycle.STARTED;
    }

    public boolean isTerminated() {
        return getServer().currentState() == IServiceLifecycle.STOPPED;
    }

    public void terminate() throws DebugException {
        if(!canTerminate())
            GODBasePlugin.throwDebugException("Cannot terminate.");
        try{
            getServer().stop();
        }catch(Exception ex){
            GODBasePlugin.throwDebugExceptionWithError("Error while shutting down remote " +
                    "processes.", ex);
        }
    }

    private synchronized void setServer(IProcessServerManager server){
        this.fProcserverManager = server;
    }
    
    private synchronized IProcessServerManager getServer(){
        return fProcserverManager;
    }
}
