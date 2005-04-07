/*
 * Created on Jul 25, 2004
 *
 */
package ddproto1.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ddproto1.exception.CommException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidStateException;
import ddproto1.exception.UnsupportedException;
import ddproto1.interfaces.IStreamGobblerListener;
import ddproto1.util.MessageHandler;
import ddproto1.util.ExternalSemaphore;
import ddproto1.util.StreamGobbler;


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
 * @author giuliano
 *
 */
public class ExpectSSHTunnel implements IShellTunnel {

    // Semaphore for helping with asynch communication
    private ExternalSemaphore sema;
    
    // State-marking constants
    public static final int UNKNOWN = -1;
    public static final int OFFLINE = 0;
    public static final int PROCESSING = 1;
    public static final int READY = 2;

    // String constants
    private static final String scriptname = "descssh.exp";
    private static final String default_prompt_regexp = "(%|#|$) $";
    private static final String module = "ExpectSSHTunnel -";
    
    private static final MessageHandler mh = MessageHandler.getInstance();

    // Attribute map for this Configurable
    private static final String[] attributes = { "host", "user", "password",
            "pass-type", "port" };

    // Connection attributes
    private String host = null;
    private String password = null;
    private String user = null;
    private String password_type = null;
    private int port = -1;

    // Result lists
    private List <String> result_oup;
    private List <String> result_err;
    
    // Shell input
    private BufferedWriter stdin;
    
    // Misc
    private String lastError;
    private int state;
    private Process shell;

    /**
     * Default constructor for ExpetSSHTunnel.
     *
     */
    public ExpectSSHTunnel(){
        // This is required for opening.
        result_oup = new LinkedList<String>();
        result_err = new LinkedList<String>();
        sema = new ExternalSemaphore(0, this);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.ShellTunnel#feedCommand(java.lang.String)
     * TODO Must add time limit control to feedCommand, close and open.
     */
    public synchronized void feedCommand(String s) throws CommException, InvalidStateException {
        
        if (state != READY)
            throw new InvalidStateException(module + " Cannot execute operation while not in READY state.");

        if (!s.endsWith("\n"))
                throw new CommException(module
                        + " Error - Commands must end in \\n");

        try {
            state = PROCESSING;
            mh.getStandardOutput().print(module + " Issuing command - " + s);
            stdin.write(s); 
            stdin.flush();
            sema.p();
        } catch (IOException e) {
            mh.printStackTrace(e);
            state = UNKNOWN;
            throw new CommException(
                    module
                            + " Error while attempting to send command to process - got I/O Exception");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.ShellTunnel#getStdoutResult()
     */
    public synchronized List getStdoutResult() throws InvalidStateException {
        /**
         * This isn't really what one could expect from this method. It returns
         * the results produced so far, without any concerns that this data
         * might be "incomplete". The whole issue arrises because we cannot
         * really know wether the attached process running in the shell is
         * finished with its output or not. One possible solution would be
         * allowing the user to specify some sort of regular expression where we
         * could "finish" reading the output, but that would complicate things
         * since Java support for this sort of operation is pretty limited. I'll
         * leave this one in as to do.
         * 
         * TODO We should add regexp-termination support for the stream processors.
         * Lexycal analyzers do just that. We could generate some lexers at run-time. 
         */
        List current_result = result_oup;
        result_oup = new LinkedList <String>();
        return current_result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.ShellTunnel#getStderrResult()
     */
    public synchronized List getStderrResult() throws InvalidStateException {
        // This operation is not supported because of the way expect works
        throw new InvalidStateException(module + " Error - Expect routes stderr to stdout.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddproto1.interfaces.Tunnel#open()
     */
    public synchronized void open() throws CommException, ConfigException {
        // First, validade parameters.
        if ((host == null) || (password == null) || (user == null)
                || (password_type == null) || (port == -1)) { throw new ConfigException(
                module + " A required parameter is missing."); }

        if (state != OFFLINE)
            throw new ConfigException(
                    module
                            + " You cannot issue an open command on a non-closed tunnel");
        
        // Will attempt to open a connection.
        String command = scriptClasspathLookup();
        command = command + " " + password + " " + host + " " + user + " "
                + port;

        try {
            mh.getStandardOutput().println(module + " Starting shell client.");
            shell = Runtime.getRuntime().exec(command);
            state = PROCESSING;
            initStreamProcessors();
            mh.getStandardOutput().println(module + " Waiting for response.");
            sema.p();
            assert(state == READY);
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

        if (attribute.equals("host")) {
            String[] spec = value.split(":");
            if (spec.length != 2)
                    throw new IllegalAttributeException(
                            module
                                    + " Error - syntax of parameter 'host' is [host-address]:[port]");

            host = spec[0];
            try {
                port = Integer.parseInt(spec[1]);
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
        } else {
            throw new IllegalAttributeException(module + " Invalid attribute "
                    + attribute);
        }
    }

    /** This will scan the classpath looking for our expect script.
     * 
     * @return
     * @throws ConfigException
     */
    private String scriptClasspathLookup() throws ConfigException {
        String classpath = System.getProperty("java.class.path");
        String separator = File.pathSeparator;
        String namesep = File.separator;

        classpath += separator + ".";
        
        boolean match = false;
                
        String[] paths = classpath.split(separator);
        String candidate = null;
        for (int i = 0; i < paths.length; i++) {
            if(paths[i].endsWith(".jar") || paths[i].endsWith(".zip"))
                continue;
            candidate = paths[i] + namesep + scriptname;
            File f = new File(candidate);
            if (f.exists()) {
                match = true;
                break;
            }
        }

        if (!match)
                throw new ConfigException(
                        "Could not find the required script <" + scriptname
                                + "> on the current classpath");

        return candidate;
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
        Thread t2 = new Thread(stderr_processor);
        
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
     //   System.out.println(s);
        result_oup.add(s);
    }

    private synchronized void receiveStderrOutput(String s) {
    //    System.err.println(s);
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
            sema.v();
            state = OFFLINE;
        } else if (s.startsWith("descssh(201) - ")) {
            mh.getStandardOutput().println(module + " Unknown host - " + host);
            state = OFFLINE;
        } else if (s.startsWith("descssh(202) - ")) {
            mh.getStandardOutput().println(module + " Permission denied (wrong password?)");
            state = OFFLINE;
        } else {
            // BUG #1 - Expect sends some empty messages that mustn't be shown.
            if(s.equals("")) return;
            mh.getStandardOutput().println(module + " Unrecognized answer " + s + " - ignored");
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
    public String [] getAttributeKeys() {
        return attributes;
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
            return key;
        } else {
            throw new IllegalAttributeException(module + " Invalid attribute "
                    + key);
        }
    }
    
    public int getState(){
        return state;
    }
    
    public Set getAttributesByGroup(String prefix){
        throw new UnsupportedException(); 
    }
    
    public void addAttribute(String key){
        throw new UnsupportedException();
    }
}