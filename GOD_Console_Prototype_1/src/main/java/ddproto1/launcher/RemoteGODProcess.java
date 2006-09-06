/*
 * Created on Nov 28, 2005
 * 
 * file: RemoteGODProcess.java
 */
package ddproto1.launcher;

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.IObjectSpec;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.launcher.procserver.IProcessEventListener;
import ddproto1.util.MessageHandler;

public class RemoteGODProcess extends PlatformObject 
    implements IProcess, IProcessEventListener, IStreamsProxy {
    
    private static final Logger asyncLogger = 
        MessageHandler.getInstance().getLogger(RemoteGODProcess.class.getName() + 
                ".asyncErrorReporter"); 
    
    
    private volatile String processLabel;
    private volatile ILaunch launch;
    private volatile IRemoteProcess proc;
    
    private final StreamMonitorImpl stdout = new StreamMonitorImpl();
    private final StreamMonitorImpl stderr = new StreamMonitorImpl();  
        
    private volatile boolean terminated = false;
    private volatile int exitValue;
    
    private final Map <String, String> attributes = 
        Collections.synchronizedMap(new HashMap<String, String>());
    
    protected RemoteGODProcess(String label, ILaunch launch){
        this.processLabel = label;
        this.launch = launch;
    }
    
    protected void setProcess(IRemoteProcess proc){
        this.proc = proc;
    }

    public String getLabel() { return processLabel; }

    public ILaunch getLaunch() { return launch; }

    public IStreamsProxy getStreamsProxy() {
        return this;
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public int getExitValue() 
        throws DebugException {
        
        if(isTerminated()) return exitValue;
        GODBasePlugin.
            throwDebugException("Can't retrieve exit value for running process.");
        
        throw new InternalError();
    }

    public Object getAdapter(Class adapter) {
        if(adapter == RemoteGODProcess.class ||
                adapter == IProcess.class) return this;

        return null;
    }

    public boolean canTerminate() {
        return !isTerminated(); // can terminate if not dead.
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() throws DebugException {
        try{
            terminated();
        }catch(ConnectException ex){
            // Okay, process server is dead. We're probably
            // quitting Eclipse.
        }catch(Exception ex){
            GODBasePlugin
                    .throwDebugExceptionWithError(
                            "Error while attempting to terminate process "
                                    + getLabel(), ex);
        }
    }
    
    public void notifyProcessKilled(int exitValue) {
        try{
            terminated();
        }catch(Exception ex){
            asyncLogger.error("Error while cleaning up dead process.", ex);
        }
        fireTerminateEvent();
    }
    
    private void terminated()
        throws RemoteException
    {
        synchronized(this){
            if(isTerminated()) return;
            setTerminated(true);
            setExitValue(exitValue);
        }
        proc.dispose();
    }

    public void notifyNewSTDOUTContent(String data) {
        stdout.contentAppended(data);
    }

    public void notifyNewSTDERRContent(String data) {
        stderr.contentAppended(data);
    }
    
    public void write(String input) throws IOException {
        try{
            proc.writeToSTDIN(input);
        }catch(IOException ex){
            throw ex;
        }catch(Exception ex){
            throw new NestedRuntimeException(ex);
        }
    }

    public IFlushableStreamMonitor getErrorStreamMonitor() {
        return stderr;
    }

    public IFlushableStreamMonitor getOutputStreamMonitor() {
        return stdout;
    }
    
    private void setTerminated(boolean value){
        this.terminated = value;
    }
    
    private void setExitValue(int exitValue){
        this.exitValue = exitValue;
    }
    
    /**
     * Fires the given debug event.
     * 
     * @param event debug event to fire
     */
    protected void fireEvent(DebugEvent event) {
        DebugPlugin manager= DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[]{event});
        }
    }

    /**
     * Fires a creation event.
     */
    protected void fireCreationEvent() {
        fireEvent(new DebugEvent(this, DebugEvent.CREATE));
    }

    /**
     * Fires a terminate event.
     */
    protected void fireTerminateEvent() {
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }

    private class StreamMonitorImpl implements IFlushableStreamMonitor{
        
        private final StringBuffer sBuffer = new StringBuffer();
        private volatile boolean buffered = true;
        private List<IStreamListener> listeners = 
            new CopyOnWriteArrayList<IStreamListener>();
        
        public void contentAppended(String newContent){
            sBuffer.append(newContent);
            fireStreamAppended(newContent);
        }

        public void flushContents() {
            sBuffer.setLength(0);
        }

        public void setBuffered(boolean buffer) {
            this.buffered = buffer;
        }

        public boolean isBuffered() {
            return buffered;
        }

        public String getContents() {
            return sBuffer.toString();
        }

        private void fireStreamAppended(String contents){
            for(IStreamListener listener : listeners)
                listener.streamAppended(contents, this);
        }
        
        public void removeListener(IStreamListener listener) {
            listeners.remove(listener);
        }

        public void addListener(IStreamListener listener) {
            listeners.add(listener);
        }
    }
}
