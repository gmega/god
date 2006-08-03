/*
 * Created on Jul 25, 2004
 *
 */
package ddproto1.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.ConfigException;
import ddproto1.exception.InvalidStateException;
import ddproto1.exception.commons.CommException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UnsupportedException;
import ddproto1.interfaces.IStreamGobblerListener;
import ddproto1.util.MessageHandler;
import ddproto1.util.ExternalSemaphore;
import ddproto1.util.StreamGobbler;
import ddproto1.util.traits.commons.ConversionUtil;


/**
 * This class represents my first serious attempt at integrating the
 * GNU SSH (Secure SHell) client into a Java application through 
 * a nice interface for programmers. Though it works very well in general,
 * it has some (a bit serious I'm afraid) drawbacks you should know about.
 * 
 * 1 - It mixes up stderr with stdout. This could be a serious
 *     problem if you need to know which is which at runtime. If you
 *     just need an error log, try redirecting the remote process output
 *     with 2>. If you need to capture ssh stderr in a separate channel,
 *     I'm sorry. Another side effect is that if you have two concurrent
 *     programs and both start writing on 'screen' at the same time, you'll
 *     end up with a load of interleaved crap. 
 * 
 * 2 - You shouldn't use the '&' option when issuing commands**. Well, at 
 *     least in theory. That's because you might end up returning too
 *     fast and scrambling the command prompt with program output. Since
 *     this class relies on a regular expression in order to detect the 
 *     command prompt, this will almost certainly cause your command never
 *     to return.
 * 
 *  ** Since you'll almost certainly need the & option, there is a possible 
 *     workaround (which I found out to work very well). Devise a launching 
 *     script that executes your command and delays for a few seconds (maybe
 *     two or three) before returning. This will prevent that sort of 
 *     stream scrambling I've mentioned earlier. 
 *     
 *     
 * Note: My attribute handling code sucks in many classes. I have thousands of
 * ifs that map string attribute keys into "real" attributes. I should've used
 * a hash map, but the idea of having to declare stack aliases for each attribute
 * didn't sound too glorious either. If this were python, we'd not be having this
 * kind of problem. Anyway, it looks like the hash maps would've made a better 
 * choice.
 * 
 * 
 * @author giuliano
 * 
 * @deprecated Too unreliable. Use the process server approach instead.
 *
 */
public class ExpectSSHTunnel implements IShellTunnel, IStreamMonitor {

    private static final int MAX_SCROLL_BUFFER_SIZE = 2000;
    
    // Semaphore for helping with asynch communication
    private ExternalSemaphore sema;
    
    // State-marking constants
    public static final int UNKNOWN = -1;
    public static final int OFFLINE = 0;
    public static final int PROCESSING = 1;
    public static final int READY = 2;
    
    // String constants
    private static final String scriptname = "descssh.exp";
    private static final String module = "ExpectSSHTunnel -";
    
    private static final MessageHandler mh = MessageHandler.getInstance();

    private static final Set <String> keySet = 
        Collections.unmodifiableSet(
            ConversionUtil.getInstance().toSet(
                    new String[] { "user", "pass", "pass-type",
                            "ssh-port", "local-agent-address", "local-expect-script-path"}));
    
    // Connection attributes
    private String host = null;
    private String password = null;
    private String user = null;
    private String password_type = null;
    private int port = -1;
    private String localScriptPath = null;
    
    private String lastError;

    // Stdout/stderr
    private CircularStringBuffer stdout;
    
    // Shell input
    private BufferedWriter stdin;
    
    // Misc
    private int state;
    private Process shell;
    
    // Listeners
    private List<IStreamListener> listeners;    

    /**
     * Default constructor for ExpetSSHTunnel.
     *
     */
    public ExpectSSHTunnel(){
        // This is required for opening.
        stdout = new CircularStringBuffer(MAX_SCROLL_BUFFER_SIZE);
        sema = new ExternalSemaphore(0, this);
        listeners = new LinkedList<IStreamListener>();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.ShellTunnel#feedCommand(java.lang.String)
     * TODO Must add time limit control to feedCommand, close and open.
     */
    public synchronized void feedCommand(String s) throws CommException,
            InvalidStateException {
        checkReady();
        if (!s.endsWith("\n"))
            throw new CommException(module
                    + " Error - Commands must end in \\n");

        state = PROCESSING;
        mh.getStandardOutput().print(module + " Issuing command - " + s);
        try {
            this.writeToStdinInternal(s);
        } catch (IOException e) {
            mh.printStackTrace(e);
            state = UNKNOWN;
            throw new CommException(
                    module
                            + " Error while attempting to send command to process - got I/O Exception");
        }

        sema.p();
    }
    
    public synchronized void writeToStdin(String message) throws InvalidStateException, IOException{
        checkReady();
        this.writeToStdinInternal(message);
    }
    
    private void writeToStdinInternal(String message) throws IOException {
        stdin.write(message);
        stdin.flush();
    }
    
    
    public void addStreamListener(IStreamListener listener){
        synchronized(listeners){
            listeners.add(listener);
        }
    }
    
    public boolean removeStreamListener(IStreamListener listener){
        synchronized(listeners){
            return listeners.remove(listener);
        }
    }
    
    private void checkReady() throws InvalidStateException{
    	switch(state){
    	case READY: 
    		return;
    	case OFFLINE:
            throw new InvalidStateException("Cannot carry specified operation on a closed tunnel.");
    	case PROCESSING:
            throw new InvalidStateException("Tunnel is currently processing a synchronous command, you must " +
                    "wait until it finishes.");
        default:
            throw new InvalidStateException("Tunnel state is undefined. Cannot carry out operation.");
    	}
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.Tunnel#open()
     */
    public synchronized void open() throws CommException, ConfigException {
        // First, validade parameters.
        if ((host == null) || (password == null) || (user == null)
                || (password_type == null) || (port == -1) || (localScriptPath == null)) 
        { throw new ConfigException(
                module + " A required parameter is missing."); }

        if (state != OFFLINE)
            throw new ConfigException(
                    module
                            + " You cannot issue an open command on a non-closed tunnel");
        
        // Will attempt to open a connection.
        File commandFile = scriptLookup();
        String [] command = new String[] {commandFile.toString(), password, host, user, Integer.toString(port) };

        try {
            mh.getStandardOutput().println(module + " Starting shell client.");
            // FIX: Uses parameter array form of exec to avoid problems with white spaces. 
            shell = Runtime.getRuntime().exec(command);
            state = PROCESSING;
            initStreamProcessors();
            mh.getStandardOutput().println(module + " Waiting for response.");
            sema.p();
            if(state != READY){
                throw new CommException(getLastError());
            }
        } catch (IOException e) {
            mh.getErrorOutput().println(module + " Could not open connection.");
            mh.printStackTrace(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.Configurable#setAttribute(java.lang.String,
     *      java.lang.String)
     */
    public void setAttribute(String attribute, String value)
            throws IllegalAttributeException {

        if (attribute.equals("local-agent-address")) {
            host = value;
        } else if (attribute.equals("ssh-port")){
            try {
                port = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalAttributeException(
                        module
                                + " Error - remote machine porte number must be an integer");
            }
        } else if (attribute.equals("user")) {
            user = value;
        } else if (attribute.equals("pass")) {
            password = value;
        } else if (attribute.equals("pass-type")) {
            if (value.equals("encrypted"))
                    throw new IllegalAttributeException(
                            module
                                    + " Error - encrypted passwords are not supported yet.");
            
            password_type = value;
        } else if(attribute.equals("local-expect-script-path")){
            localScriptPath = value;
        }else {
            throw new IllegalAttributeException(module + " Invalid attribute "
                    + attribute);
        }
    }

    /** Looks for the required expect script.
     * 
     * @return
     * @throws ConfigException
     */
    private File scriptLookup() throws ConfigException {
        
        /** This method used to be called scriptClasspathLookup, but now it
         * just looks for the script on a specific path. That's because Eclipse
         * uses the classpath, so we can't rely on it anymore.
         */
        
        String candidate = localScriptPath + File.separator + scriptname;
        File f = new File(candidate);
        if (!f.exists()){
            throw new ConfigException(
                    "Could not find the required script <" + scriptname
                    + ">.");
        }

        return f;
    }

    /** Preps and launches the stream-processing threads.
     * 
     *
     */
    private void initStreamProcessors() {

        StreamGobbler stdout_processor = new StreamGobbler(shell.getInputStream());
        StreamGobbler stderr_processor = new StreamGobbler(shell.getErrorStream());

        // Routes stdout notifications to receiveStdoutOutput
        stdout_processor.addStreamGobblerListener(new IStreamGobblerListener() {

            public void receiveLine(String s) {
                receiveStdoutOutput(s);
            }
        });

        // Does the same with stderr to receiveStderrOutput
        stderr_processor.addStreamGobblerListener(new IStreamGobblerListener() {

            public void receiveLine(String s) {
                receiveStderrOutput(s);
            }
        });

        // Start them.
        Thread t1 = new Thread(stdout_processor);
        t1.setName("STDOUT Stream Gobbler");
        Thread t2 = new Thread(stderr_processor);
        t1.setName("STDERR Stream Gobbler");
        
        t1.start();
        t2.start();
        
        // Attaches a bufferedwriter to the process stdin. This allows us to
        // write in chunks.
        stdin = new BufferedWriter(new OutputStreamWriter(shell.getOutputStream()));
    }
    
    /** 
     * Those are both asynchronous notification methods invoked by the
     * StreamGobblers.
     * 
     */
    private synchronized void receiveStdoutOutput(String s) {
        synchronized(stdout){
            stdout.append(s);
        }
       
        synchronized(listeners){
            for(IStreamListener listener : listeners){
                listener.streamAppended(s, this);
            }
        }
    }

    private synchronized void receiveStderrOutput(String s) {
        computeTransition(s);
    }

    private void computeTransition(String s) {
        // TODO Perhaps implement an elective notification mechanism
        /** Note that whenever we enter ready state we up the semaphore.
         * This will work, however, because p and v always will go paired.
         * We could, as a measure of precaution, use a binary semaphore instead,
         * but we won't. :-)
         */
       
        if (s.startsWith("descssh(100) - ")) {
            mh.getStandardOutput().println(module + " Added " + host + " to known host list.");
            return;
        } else if (s.startsWith("descssh(101) - ")) {
            mh.getStandardOutput().println(module + " Password sent, waiting for authorization.");
            return;
        } else if (s.startsWith("descssh(102) - ")) {
            mh.getStandardOutput().println(module + " Tunnel is open.");
            state = READY;
            sema.v();
            return;
        } else if (s.startsWith("descssh(103) - ")) {
            mh.getStandardOutput().println(module + " Awaiting command.");
            state = READY;
            sema.v();
            return;
        } else if (s.startsWith("descssh(104) - ")) {
            mh.getStandardOutput().println(module + " Connection closed.");
            state = OFFLINE;
            sema.v();
        } else if (s.startsWith("descssh(201) - ")) {
            setLastError(" Unknown host - " + host);
            mh.getStandardOutput().println(module + getLastError());
            state = OFFLINE;
            sema.v();
        } else if (s.startsWith("descssh(202) - ")) {
            setLastError("SSH connection error: Permission denied (wrong password?)");
            mh.getErrorOutput().println(module + getLastError());
            state = OFFLINE;
            sema.v();
        }else if(s.startsWith("descssh(203) - ")){
            setLastError("SSH connection error: host key verification failed. Check your known hosts file.");
            mh.getErrorOutput().println(module + getLastError());
            state = OFFLINE;
            sema.v();
        } else {
            // BUG #1 - Expect sends some empty messages that mustn't be shown.
            if(s.equals("")) return;
            mh.getWarningOutput().println(module + " Unrecognized answer " + s + " - ignored");
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Tunnel#close()
     */
    public synchronized void close(boolean forced) throws CommException, ConfigException {

        if (state == OFFLINE) return;

        if (!forced) {
            if (state != READY) { throw new InvalidStateException(
                    module
                            + " You must wait until running command terminates before closing the tunnel."); }
            feedCommand("logout\n");
        }
        shell.destroy();
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#getAttributeKeys()
     */
    public Set<String> getAttributeKeys() {
        return keySet;
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#getAttributeValue(java.lang.String)
     */
    public String getAttribute(String key) throws IllegalAttributeException {
        if (key.equals("host")) {
            return host;
        } else if (key.equals("user")) {
            return user;
        } else if (key.equals("pass")) {
            return password;
        } else if (key.equals("pass-type")) {
            return password_type;
        } else if(key.equals("local-expect-script-path")){
            return localScriptPath;
        }else if (key.equals("local-agent-address")) {
            return host;
        } else {
            throw new IllegalAttributeException(module + " Invalid attribute "
                    + key);
        }
    }
    
    public int getState(){
        return state;
    }

    public boolean isWritable() {
        return true;
    }

    public String getStderrContents() {
        return null;
    }

    public String getStdoutContents() {
        synchronized(stdout){
            return stdout.toString();
        }
    }

    public void addListener(IStreamListener listener) {
        this.addStreamListener(listener);
    }

    public String getContents() {
        return stdout.toString();
    }

    public void removeListener(IStreamListener listener) {
        this.removeStreamListener(listener);
    }
    
    private void setLastError(String s){
        lastError = s;
    }
    
    private String getLastError(){
        return lastError;
    }
    
    private void clearLastError(){
        lastError = null;
    }
}