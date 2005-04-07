 /*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Console.java
 */

package ddproto1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.NodeInfo;
import ddproto1.debugger.eventhandler.processors.IJDIEventProcessor;
import ddproto1.debugger.managing.AttributeTranslator;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.managing.tracker.VirtualStackframe;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InternalError;
import ddproto1.exception.CommandException;
import ddproto1.exception.NoSuchElementError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.UnsupportedException;
import ddproto1.interfaces.IUICallback;
import ddproto1.launcher.IApplicationLauncher;
import ddproto1.lexer.Lexer;
import ddproto1.lexer.Token;
import ddproto1.sourcemapper.ISource;
import ddproto1.sourcemapper.ISourceMapper;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.collection.ThreadGroupIterator;
import ddproto1.util.traits.ConversionTrait;
import ddproto1.util.traits.JDIMiscTrait;

/**
 * This highly inflated class represents our first attempt at constructing
 * a user interface to the DD API. It serves as a text interface in a GDB/JDB
 * style from where one can type in commands and observe results. 
 * 
 * @author giuliano
 *
 */
public class ConsoleDebugger implements IDebugger, IUICallback{

    private static final String module = "ConsoleDebugger -";
    private static final String iprompt = "command >";
    private static final int SHOW_DEFAULT = 5;
    private static final MessageHandler mh = MessageHandler.getInstance();
    
    private String lastCommand = null;
    private String currentCommand = null;
    private Properties descriptions;
    private Token token;
    private Lexer l;
    
    private boolean isPromptPrinted = false;
    private boolean globalMode = false;
    private boolean running;
    
    private Map vmid2requests = new HashMap();
    
    private Map <String, NodeInfo> vmid2ninfo = new HashMap <String, NodeInfo> ();
    private String currentMachine = null;
    private String prompt;
    private LinkedList <String> batch = new LinkedList <String> ();
    
    private static ConversionTrait ct = ConversionTrait.getInstance();
    private static JDIMiscTrait jmt = JDIMiscTrait.getInstance();
    
    /* -----------------------------------------------------  */
    /* This will ease up things a bit, even though it's ugly. */ 
    private static Map <String, Integer> stringtokens;
    static{
        stringtokens = new HashMap<String, Integer>();
        stringtokens.put("line", new Integer(StepRequest.STEP_LINE));
        stringtokens.put("instruction", new Integer(StepRequest.STEP_MIN));
        stringtokens.put("into", new Integer(StepRequest.STEP_INTO));
        stringtokens.put("over", new Integer(StepRequest.STEP_OVER));
        stringtokens.put("return", new Integer(StepRequest.STEP_OUT));
        stringtokens.put("thread", new Integer(Token.THREAD));
        stringtokens.put("nokill", new Integer(Token.EXIT));
    }
    
    private static int getMode(String md)
    	throws CommandException
    {
        Integer mode = (Integer)stringtokens.get(md);
        if(mode == null){
            throw new CommandException("Invalid parameter - " + md);
        }
        return mode.intValue();
    }
    
    private static boolean isRepeatable(int c){
        if(c != Token.STEP)
            return false;
        
        return true;
    }
    /* -----------------------------------------------------  */
    
    /** Default constructor for class ConsoleDebugger - creates a new
     * Console Debugger. This class assumes that the default MessageHandler
     * is already set.
     */
    public ConsoleDebugger() { }
    
    public synchronized void printLine(String s){
        if(isPromptPrinted){
            System.out.println("\n" + s + "\n");
            System.out.print(prompt + iprompt);
        }else{
            System.out.println(s);
        }
    }
    
    public synchronized void printMessage(String s){
        if(isPromptPrinted){
            System.out.println("\n" + s);
            System.out.print(prompt + iprompt);
        }else{
            System.out.print(s);
        }
    }
    
    /**
     * Starts the main loop. This function returns only if the user types exit.
     */
    public void mainLoop() {
        // Loads command descriptions
        descriptions = new Properties();
        try {
            descriptions.load(new FileInputStream("text/help.properties"));
        } catch (Exception e) {
            mh.getErrorOutput().println("Error - cannot read help file. No help from commands will be avaible");
        }
    
        // Blocks a few methods
        running = true;
    
        // Initializes input stream from standard input 
        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        StringReader reader = new StringReader("Aaaah dummy dummy....");
        l = new Lexer(reader);
    
    
        // Console loop.
        while (running) {
            prompt = (currentMachine == null)? "" : currentMachine + "/";
            
            synchronized(this){
                mh.getStandardOutput().print(prompt + iprompt);
                isPromptPrinted = true;
            }
            
            try {

                String command;
 
                // Old read-parse scheme
                if(batch.isEmpty()){
                    command = stdin.readLine();
                }else{
                    command = (String)batch.removeFirst();
                    mh.getStandardOutput().print(command + "\n");
                }
                
                synchronized(this){
                    isPromptPrinted = false;
                }
                
                reader = new StringReader(command);
                currentCommand = command;
                l.yyreset(reader);
                if (!readParse())
                    mh.getStandardOutput().println(
                            "Unknown command \"" + command + "\"");
    
            } catch (CommandException e) {
                mh.getStandardOutput().println(e.toString());
                lastCommand = null;
            } catch (Exception e) {
                mh.printStackTrace(e);
                lastCommand = null;
                mh.getStandardOutput().print("\n" + prompt + iprompt);
            }
        }
    }
    
    /**
     * Parses a string loaded in the lexycal analyzer.
     * 
     * @return true se o comando for válido, false caso contrário.
     * @throws IOException
     * @throws CommandException
     * @throws ConfigException
     */
    private boolean readParse() throws IOException, CommandException,
            ConfigException {

        String machine = null;
        
        advance();
   
        int commandCode = token.type;
        
        switch (commandCode) {

        /**
         * Command RUN - Launches a JVM and then attaches to it.
         * 
         * run (machine_id)? | run
         *  
         */
        case Token.RUN: {
            machine = checkCurrent("command.run");
            assert(machine != null); // Postcondition
            
            /* User typed a machine ID */
            if (token.type == Token.MACHINE_ID || token.type == Token.EMPTY) {
                commandLaunch(machine);
                commandAttach(machine);
            } 
            /* User typed "all" */
            else if (token.type == Token.ALL) {
                Iterator it = vmid2ninfo.keySet().iterator();
                while (it.hasNext()) {
                    commandLaunch((String) it.next());
                    commandAttach((String) it.next());
                }
            }else{
                badCommand("command.run");
            }

            break;
        }
        
        case Token.RUNSCRIPT:
        {
            String [] comm = currentCommand.split(" ");
            if(comm.length < 2)
                badCommand("command.script");
            
            this.printLine("Running script " + comm[1]);
            commandRunscript(comm[1]);
            
            break;
        }

        /**
         * Command BREAKPOINT - Inserts a breakpoint.
         * 
         * break [machine_id]? ClassName.method(ClassName1, ClassName2, ...)
         */
        case Token.BREAKPOINT: {
            machine = checkCurrent("command.break");
            commandBreakpoint(machine);
            break;
        }

        /**
         * Command STEP - Steps a thread or the current thread (for a give JVM).
         * 
         * step line|instruction|return into|over [machine_id]? threadId?
         */
        case Token.STEP: {
            int depth, granularity = Token.UNIDENTIFIED;
            String hexy = null;
            
            advance();
            if (!(token.type == Token.WORD))
                badCommand("command.step");
            depth = ConsoleDebugger.getMode(token.text);

            advance();
            if (token.type == Token.WORD) {
                granularity = depth;
                depth = ConsoleDebugger.getMode(token.text);
                advance();
            }
            
            if (token.type == Token.MACHINE_ID) {
                machine = token.text.substring(1, token.text.length() - 1);
                advance();
            }

            if (machine == null) {
                if (currentMachine == null) {
                    throw new CommandException(
                            "There's currently no machine selected. You must spe"
                                    + "cify a machine id.");
                } else {
                    machine = currentMachine;
                }
            }

            if (token.type == Token.HEX_ID) {
                if(machine.equals("Global"))
                    throw new CommandException("Cannot use HEX ids while in global mode.");
                hexy = token.text;
            }else if((token.type == Token.DOTTED_ID)){
                if(!machine.equals("Global"))
                    throw new CommandException("Cannot use DOOTED ids while in local mode.");
               hexy = token.text;
            }


            if (granularity == Token.UNIDENTIFIED
                    && depth != StepRequest.STEP_OUT)
                throw new CommandException(
                        "You must specify a granularity for your step request.");
            
            
            if(machine.equals("Global")) commandStepGlobal(granularity, depth, hexy); 
            else commandStep(machine, granularity, depth, hexy);
            lastCommand = currentCommand;

            break;
        }

        /**
         * Command LAUNCH - Launches a given JVM (launches means that it starts
         * a process).
         * 
         * launch (machine_id)?
         */
        case Token.LAUNCH: {
            machine = checkCurrent("command.launch");
            commandLaunch(machine);
            break;
        }

        /**
         * Command ATTACH - Attaches to an already running JVM.
         * 
         * attach (machine_id)?
         */
        case Token.ATTACH: {
            machine = checkCurrent("command.attach");
            commandAttach(machine);
            mh.getStandardOutput().println("Attach successful.");
            break;
        }

        /**
         * Command INFO - Shows information about a node.
         * 
         * info (machine_id)?
         */
        case Token.INFO: {
            machine = checkCurrent("command.info");
            commandInfo(machine);
            break;
        }

        /**
         * Command SELECT - Selects a node for subsequent commands.
         * 
         * select machine_id
         *  
         */
        case Token.SELECT: {
            advance();
            if (token.type != Token.MACHINE_ID)
                badCommand("command.select");

            String temp = token.text.substring(1, token.text.length() - 1);
            if(temp.equals("Global")){
                globalMode = true;
                mh.getStandardOutput().println("Global mode selected.");
                currentMachine = "Global";
                break;
            }
            
            VirtualMachineManager vmm = VMManagerFactory.getInstance()
                    .getVMManager(temp);
            if (vmm == null) {
                mh.getStandardOutput().println(
                        "Node <" + temp + "> is invalid.");
                break;
            }
            mh.getStandardOutput().println(token.text + " selected\n");
            currentMachine = temp;
            break;
        }

        /**
         * Command LIST - Lists available nodes or information about threads
         * running on a given node.
         * 
         * ls threads|threadgroups
         *  
         */
        case Token.LIST: {
            advance();
            int mode = token.type;

            if (!(token.type == Token.EMPTY))
                mode = ConsoleDebugger.getMode(token.text);

            commandList(mode);

            break;
        }
        
        /**
         * Command RESUME - Resumes a local thread.
         * 
         * resume (machine_id)? thread_id
         *  
         */
        case Token.RESUME:
            
            advance();
            machine = checkCurrent("command.resume", false);
        
        	if(token.type == Token.MACHINE_ID) advance();
            
            if(token.type == Token.DOTTED_ID && machine.equals("Global")){
                commandResumeDT(token.text);
                break;
            }
            
            if(!(token.type == Token.HEX_ID))
        	    badCommand("command.resume");
        	
        	commandResume(machine, token.text);
        	
        	break;
        	
        /**
         * Command SUSPEND - Suspends a local thread.
         *  
         * suspend (machine_id)? thread_id
         */
        case Token.SUSPEND:
            
            machine = checkCurrent("command.suspend");
        
        	if(token.type == Token.MACHINE_ID) advance();
            
        	if(!(token.type == Token.HEX_ID))
        	    badCommand("command.suspend");
        	
        	commandSuspend(machine, token.text);
            break;

        /**
         * Command EXIT - Exits the debugger. User must specify wether we should
         * just disconnect from debugee JVMs or kill them all.
         * 
         * exit nokill|kill
         *  
         */
        case Token.EXIT: {
            advance();

            int mode = token.type;

            if (token.type != Token.KILL) {
                mode = ConsoleDebugger.getMode(token.text);
            }

            shutdown(mode);
            break;
        }

        /**
         * Command SHOW - Shows source information around the current location
         * for a given thread or displays stack information for that thread.
         * 
         * show (radius|"stack")? (machine_id)? (thread_id)? 
         * show "stack" dotted_id
         */
        case Token.SHOW: {

            String hexy = null;
            int radius = SHOW_DEFAULT;
            boolean stack = false;

            advance();
            if (token.type == Token.NUMBER) {
                radius = Integer.parseInt(token.text);
                advance();
            }else if(token.type == Token.WORD && token.text.equals("stack")){
                stack = true;
                advance();
            }
            
            if(!currentMachine.equals("Global")){
                machine = checkCurrent("command.show", false);

                if (token.type == Token.MACHINE_ID)
                    advance();

                if (token.type == Token.HEX_ID)
                    hexy = token.text;
            

                advance();
            	if (!(token.type == Token.EMPTY))
                	badCommand("command.show");

            	if(!stack) commandShow(machine, radius, hexy);
            	else commandShowStackLT(machine, hexy);

            }else{
                if(token.type != Token.DOTTED_ID)
                    badCommand("command.show");
                
                if(!stack) commandShowDT(token.text);
                else commandShowStackDT(token.text);
            }


            break;
        }
        
        /**
         * Command KILL - Terminates the target VM (if attached).
         * 
         * (machine_id)?
         */
        case Token.KILL:{
            machine = checkCurrent("command.kill");
            commandKill(machine);
            break;
        }

        /**
         * Allows for repeatable commands.
         */
        case Token.EMPTY: {
            if (!(lastCommand == null)) {
                currentCommand = lastCommand;
                lastCommand = null;
                StringReader reader = new StringReader(currentCommand);
                l.yyreset(reader);
                mh.getStandardOutput().println(currentCommand);
                readParse();
                
                return true;
            }
            break;
        }

        default:
            return false;
        }
        
        if(!isRepeatable(commandCode))
            lastCommand = null;
        else
            lastCommand = currentCommand;
        
        return true;

    }
    
    /*
      -------------------------------------------------------------------------
      ---------------------- Command handlers ---------------------------------
      -------------------------------------------------------------------------
     */
    
    /**
     * Prints information about a JVM.
     * 
     * @param <b>string</b> JVM id.
     */
    private void commandInfo(String vmid) throws CommandException {
        VMManagerFactory vmf = VMManagerFactory.getInstance();
        VirtualMachineManager vmm = vmf.getVMManager(vmid);
        if (vmm == null)
            throw new CommandException(" Cannot launch: Invalid JVM id " + vmid
                    + " specified.");
    
        mh.getStandardOutput().println(vmm.toString());
    }

    /**
     * Connects to a (remote) JVM.
     * 
     * @param <b>string</b> id of the JVM to connect to.
     */
    private void commandAttach(String vmid) throws CommandException {
        VMManagerFactory vmf = VMManagerFactory.getInstance();
        VirtualMachineManager vmm = vmf.getVMManager(vmid);
        if(vmm == null)
        	throw new CommandException(" Cannot attach: Invalid JVM id " + vmid
                    + " specified.");

        try {
            vmm.connect();
        } catch (Exception e) {
            mh.printStackTrace(e);
            throw new CommandException("Attach failed.");
        }
    }

    /**
     * Launches a JVM.
     * 
     * @param <b>vmid</b> id of the JVM to launch.
     */
    private void commandLaunch(String vmid) throws CommandException,
            ConfigException {

        mh.getStandardOutput().println(" Now launching JVM <" + vmid + ">");

        NodeInfo ninfo = (NodeInfo) vmid2ninfo.get(vmid);
        try {
            // Checks to see if all required attributes have been set (not
            // giving a damn about their consistency).
            ninfo.allSet();

            // Loads the specified application launcher class.
            String launcher = ninfo.getAttribute("launcher-class");
            String tunnel = ninfo.getAttribute(launcher + ".tunnel-class");

            String cname = null;

            try {
                Class c = Class.forName(launcher);
                IApplicationLauncher l = (IApplicationLauncher) c.newInstance();
                
                // Idiossyncratic behaviour - must set tunnel-class first.
                l.setAttribute("tunnel-class", ct.extractPrefix(tunnel, launcher));

                // Passes the parameters through.
                String[] keys = ninfo.getAttributeKeys();
                for (int i = 0; i < keys.length; i++) {
                    cname = keys[i];
                    String remove;
                    if (keys[i].startsWith(launcher)) {
                        l.setAttribute(ct.extractPrefix(keys[i], launcher), ninfo
                                .getAttribute(keys[i]));
                    }
                }
                // Finally, launches the application.
                l.launch();

            } catch (ClassNotFoundException e) {
                throw new ConfigException(" Launcher class " + launcher
                        + " could not be found. Check your classpath.");
            } catch (IllegalAccessException e) {
                throw new ConfigException(
                        " Could not obtain access to default constructor in class "
                                + launcher + ". Make shure it's public.");

            } catch (IllegalAttributeException e) {
                throw new ConfigException(
                        " Internal error! - Illegal launcher argument " + e.toString());

            } catch (InstantiationException e) {
                throw new ConfigException(e.toString());
            }

        } catch (Exception e) {
            mh.printStackTrace(e);
            throw new CommandException("Launch failed.");
        }
        
    }
    
    /**
     * Inserts a breakpoint at a given thread in a given machine.
     * 
     * Expects: Token.BREAKSPEC as next token in the command buffer.
     * 
     * @param <b>string</b> id of the JVM where the thread we wish to 
     * insert the breakpoint into is located.
     */
    private void commandBreakpoint(String machine) throws CommandException, IOException {
        
        boolean isMethod = false;
        
        // Maybe checkcurrent already advanced for us.
        if(token.type != Token.METHOD_BREAKSPEC && token.type != Token.CLASS_BREAKSPEC)
            advance();
        
        if (token.type == Token.METHOD_BREAKSPEC) isMethod = true;
        else if (token.type == Token.CLASS_BREAKSPEC) isMethod = false;
        else badCommand("command.break");

        
        /* Starts by obtaining a reference to our VMM (eagerly checks if
           targeted JVM exists. */
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        if(vmm == null)
            throw new CommandException(" Cannot set breakpoint for unregistered JVM <" + machine + ">");
        
        String[] bspec = token.text.split(":");
        String classOrMethod = bspec[0];
        int lineNumber = Integer.parseInt(bspec[1]);

        try {
            DeferrableBreakpointRequest dbr = null;
            if (isMethod) {
                /*
                 * The nice thing about user lexers is that you can rely on the
                 * fact that your method spec is valid.
                 */
                int parens = classOrMethod.indexOf('(');
                String methodname = classOrMethod.substring(0, parens);
                String arglist = classOrMethod.substring(parens + 1, classOrMethod
                        .length() - 1);

                /*
                 * Arglist required for overloaded methods (since we can't tell
                 * wether a method is overloaded or not until runtime, we pass
                 * the parameter list whenever there are parameters to pass)
                 */
                List<String> args = new LinkedList<String>();
                StringTokenizer st = new StringTokenizer(arglist, ",");
                // Insert them in order
                for (int i = 0; st.hasMoreTokens(); i++)
                    args.add(i, st.nextToken());

                int lineNo = Integer.parseInt(bspec[1]);

                // Now, creates a breakpoint spec.
                dbr = new DeferrableBreakpointRequest(machine, methodname,
                        args, lineNo);
            } else {
                dbr = new DeferrableBreakpointRequest(machine, classOrMethod,
                        lineNumber);
            }

            // Sets it into the targeted vm
            if (!vmm.getDeferrableRequestQueue().addEagerlyResolve(dbr))
                mh.getStandardOutput().println("Set deferred breakpoint");

        } catch (Exception e) {
            mh.printStackTrace(e);
            mh.getErrorOutput().println("Failed to add breakpoint.");
        }
    }

    /**
     * Lists all the virtual machines that have been configured 
     * and their respective threads.
     * 
     */
    private void commandList(int what) 
    	throws CommandException, IOException
    {
        VMManagerFactory vmmf = VMManagerFactory.getInstance();
        
        advance();
        if(what == Token.THREAD){
            if(currentMachine == null){
                throw new CommandException("You must select a node before listing its threads");
            }else if(currentMachine.equals("Global")){
                listGlobalThreads();
                return;
            }
            VirtualMachineManager vmm = vmmf.getVMManager(currentMachine); 
            if(!vmm.getThreadManager().isVMSuspended()){
                mh.getStandardOutput().println("Warning - JVM is not suspended. This list may not be accurate.");
            }
            mh.getStandardOutput().println("Threads for node <" + currentMachine + ">");
            listThreads();
                        
        } else {

            // Lists machines and their status.
            Iterator it = vmmf.machineList();

            mh
                    .getStandardOutput()
                    .println(
                            "The following JVM nodes have been configured (but not necessarily launched):");
            for (int i = 0; it.hasNext(); i++) {
                VirtualMachineManager vmm = (VirtualMachineManager) it.next();
                mh.getStandardOutput().println(i + " - " + vmm.getName());
            }
        }
    }

    private void commandStepGlobal(int size, int depth, String dotted_id)
    	throws CommandException
    {
        Integer dt_uuid = new Integer(ct.dotted2Uuid(dotted_id));
        Integer lt_uuid;
        Byte machineGID = new Byte(ct.dotted2MachineID(dotted_id));
        VMManagerFactory vmmf = VMManagerFactory.getInstance();
        
        DistributedThreadManager dtm = Main.getDTM();
        VirtualMachineManager vmm;
        
        DistributedThread dt = null;
        
        /* Thread is a distributed thread. */
        if(dtm.existsDT(dt_uuid)){
            try{
                dt = dtm.getByUUID(dt_uuid);
                dt.lock();
                if(!dt.isSuspended())
                    throw new CommandException("You can't place a step request in a running thread.");
                VirtualStackframe vsf = dt.virtualStack().peek();
                vmm = vmmf.getVMManager(vsf.getLocalThreadNodeGID());
                lt_uuid = vsf.getLocalThreadId();
            }catch(NoSuchElementError e){
                throw new CommandException("Thread died while attempting to get it.");
            }finally{
                if(dt != null && dt.isLocked()) dt.unlock();
            }
        }
        /* This thread is NOT a distributed thread */
        else{
            vmm = vmmf.getVMManager(machineGID);
            lt_uuid = dt_uuid;
        }
        
        IVMThreadManager tm = vmm.getThreadManager();
        ThreadReference target = tm.findThreadByUUID(lt_uuid);
        if(target == null)
            throw new CommandException("Thread doesn't exist.");
        
        doStep(vmm, target, size, depth);
        
        target.resume();
    }
    
    /**
     * 
     */
    private void commandStep(String machine, int size, int depth, String hexy) 
    	throws CommandException, IOException
    {

        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        ThreadReference target = null;
    
        if(size == -1)
            size = StepRequest.STEP_MIN;
        
        // If there's a thread id, then we'll stop a specific thread.
        target = grabSuspendedThread(hexy, vmm);
        if(target == null)
            throw new CommandException("Thread doesn't exist.");

        
        doStep(vmm, target, size, depth);
        
        target.resume();
    }
    
    private void doStep(VirtualMachineManager vmm, ThreadReference target, int size, int depth){
        PolicyManager pm = PolicyManager.getInstance();
        /* This is a bizarre action we have to take in order to avoid DuplicateRequestExceptions */
        jmt.clearPreviousStepRequests(target);
        
        /* Step requests are not deferrable. Threrefore, they're set directly
         * into the VirtualMachine, without using VirtualMachineManger#makeRequest.
         */
        VirtualMachine jvm = vmm.virtualMachine();
        StepRequest sr = jvm.eventRequestManager().createStepRequest(target, size, depth);
        sr.addCountFilter(1);
        sr.setSuspendPolicy(pm.getPolicy("request.step"));
        sr.putProperty(DebuggerConstants.VMM_KEY, vmm.getName());
        sr.enable();
    }
    
    private void commandShowDT(String dottedid){
        
    }
    
    private void commandShowStackDT(String dottedid) 
    	throws CommandException
    {
        try {
            Integer dt_uuid = new Integer(ct.dotted2Uuid(dottedid));
            DistributedThreadManager dtm = Main.getDTM();

            DistributedThread dt = dtm.getByUUID(dt_uuid);

            assert (dt != null);

            mh.getStandardOutput().println(
                    "Stack information for distributed thread " + "["
                            + dottedid + "]");

            DistributedThread.VirtualStack vs = dt.virtualStack();

            String stck = "";
            int acc = 0;
            for (int i = 0; i < vs.getFrameCount(); i++) {
                VirtualStackframe vf = vs.getFrame(i);
                VirtualMachineManager vmm = VMManagerFactory.getInstance()
                        .getVMManager(vf.getLocalThreadNodeGID());

                ThreadReference tr = vmm.getThreadManager().findThreadByUUID(
                        vf.getLocalThreadId());
                Integer callBase = vf.getCallBase();
                Integer callTop = vf.getCallTop();
                boolean doResume = false;
                if(!tr.isSuspended()){
                    tr.suspend();
                    doResume = true;
                }
                int base = callBase.intValue();
                int top = callTop.intValue();
                if(base == -1) base = 1;
                if(top == -1) top = tr.frameCount();
                stck += threadStack(tr, base, top, acc, vmm.getName());
                acc += top - base + 1;
                if(doResume) tr.resume();
            }
            
            mh.getStandardOutput().print(stck);
            
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }
    
    private void commandShow(String machine, int radius, String hexid)
    	throws CommandException, IOException
    {
   
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        ThreadReference target = null;
        
        target = grabSuspendedThread(hexid, vmm);
        
        try{
            Location loc = target.frame(0).location();
            ISourceMapper sm = vmm.getSourceMapper();
            ISource is = sm.getSource(loc);
            int line = loc.lineNumber();
            int i = (line - radius) > 0? line - radius:0;

            for(; i < (line + radius); i++){
                String linestr = "["+i+"]: ";
                if(i == line) linestr = "["+i+"]=>";
                mh.getStandardOutput().println(linestr + is.getLine(i));
            }
           
        }catch(AbsentInformationException e){
            throw new CommandException("No valid source-code information found.");
        }catch(Exception e){
            mh.printStackTrace(e);
            throw new CommandException("Failed to obtain thread location.");
        }	
    }
    
    private void commandShowStackLT(String machine, String hexy)
    	throws CommandException
    {
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        ThreadReference target = null;
       
        target = grabSuspendedThread(hexy, vmm);
        try{
            mh.getStandardOutput().println(
                    "Stack information for thread " + "[" + target.name()
                            + "], id [" + hexy + "]");
            
            mh.getStandardOutput().println(threadStack(target, 1, target.frameCount(), -1, null));
            
        }catch(IncompatibleThreadStateException e){
            throw new CommandException(e);
        }
    }
    
    private String threadStack(ThreadReference target, int base, int top, int begin, String machine)
    	throws IncompatibleThreadStateException
    {
        
        StringBuffer pr = new StringBuffer();
        
        base = Math.max(0, target.frameCount() - base);
        top = Math.max(0, target.frameCount() - top);
        
        assert(top <= base);
        
        
        String space = null;
        if(machine != null){
            StringBuffer spacing = new StringBuffer(machine.length() + 6);
            for(int i = 0; i < machine.length() + 6; i++)
                spacing.append(" ");
            space = spacing.toString();
        }
        
        for(int i = top; i <= base; i++, begin++){
            Location loc = target.frame(i).location();
            
            Method m = loc.method();

            if(machine != null){
                if(i == top || i == base) pr.append(((i == (top+base)/2)?"["+machine+"]<---":space) + "+---->");
                else pr.append( (i == (top+base)/2)?("["+machine+"]<---|     "):(space + "|     "));
            }

            pr.append(begin + " " + loc.declaringType().name() + "."
                            + m.name() + "(");
            
            
            Iterator it = 
                m.argumentTypeNames().iterator();
            	
            while(it.hasNext()){
                pr.append((String)it.next());
                if(it.hasNext()) pr.append(",");
            }
            
            
            pr.append(")\n");
        }
        
        return pr.toString();
    }
    
    private void commandRunscript(String file)
    	throws CommandException
    {
        try{
            File script = new File(file);
            if (!script.exists())
                throw new CommandException("Script file " + file
                        + " could not be found.");
            
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    new FileInputStream(script)));
            
            int i = 0;
            
            this.printMessage("Loading script " + file + " into batch command list...");
            
            String batchCommand;
                        
            while((batchCommand = input.readLine()) != null){
                batch.addLast(batchCommand);
                i++;
            }
            
            this.printLine("[ "+ i + " command(s) loaded]");
            
        }catch(IOException e){
            throw new CommandException(e);
        }
    }
    
    private void commandSuspend(String machine, String hexy)
    	throws CommandException
    {
        VirtualMachineManager vmm = VMManagerFactory.getInstance()
                .getVMManager(machine);

        /* Attempts to acquire a reference to preexisting thread */
        long uid = ct.hex2Long(hexy);
        ThreadReference target = vmm.getThreadManager().findThreadById(uid);

        /* Checks if thread really exists and if it's already been suspended */
        if (target == null)
            throw new CommandException("Invalid thread id <" + hexy
                    + "> for machine " + machine);
        else if (target.isSuspended())
            throw new CommandException(
                    "Cannot suspend a thread that has already been suspended.");

        /* All fine. Suspend it. */
        target.suspend();
    }
    
    private void commandResume(String machine, String hexy) 
    	throws CommandException
    {
        VirtualMachineManager vmm = VMManagerFactory.getInstance()
                .getVMManager(machine);

        /* Attempts to acquire a reference to preexisting thread */
        ConversionTrait sh = ConversionTrait.getInstance();
        long uid = sh.hex2Long(hexy);
        ThreadReference target = vmm.getThreadManager().findThreadById(uid);

        /* Checks if thread really exists and if it's really suspended */
        if (target == null)
            throw new CommandException("Invalid thread id <" + hexy
                    + "> for machine " + machine);
        else if (!target.isSuspended())
            throw new CommandException(
                    "Cannot resume a running thread.");

        /* All fine. Resume it. */
        target.resume();

    }
    
    private void commandResumeDT(String dottedid)
        throws CommandException
    {
        Integer dt_uuid = ct.dotted2Uuid(dottedid);
        DistributedThread dt = Main.getDTM().getByUUID(dt_uuid);
        
        dt.lock();

        try{
            if(!dt.isSuspended()){
                throw new CommandException(
                    "Cannot resume a running thread.");
            }else
                dt.resume();
        }finally{
            dt.unlock();
        }
    }
    
    private void commandKill(String machine){
        VirtualMachineManager vmm = VMManagerFactory.getInstance()
        	.getVMManager(machine);
        
        vmm.virtualMachine().exit(0);
        mh.getStandardOutput().print("Virtual machine <" + machine + "> has been killed.");
    }
    
    private ThreadReference grabSuspendedThread(String hexy, VirtualMachineManager vmm)
    	throws CommandException
    {
        VirtualMachine jvm = vmm.virtualMachine();
        IVMThreadManager tm = vmm.getThreadManager();

        ThreadReference target = null;
        
        String stats;
        
        if(hexy != null){
            ConversionTrait sh = ConversionTrait.getInstance();
            long uid = sh.hex2Long(hexy);
            target = vmm.getThreadManager().findThreadById(uid);
            stats = "Invalid thread id (maybe thread already died?)";
        }else{
            target = vmm.getThreadManager().getCurrentThread();
            stats = "No current thread set. You must specify which thread to step.";
        }
        
        // Only suspended threads are eligible.
        if(!target.isSuspended()){
            ConversionTrait sh = ConversionTrait.getInstance();
            stats = "You must suspend the thread " + sh.long2Hex(target.uniqueID()) + " before attempting to execute this operation.";
            target = null;
        }
        
        if(target == null)
            throw new CommandException(stats);
        
        return target;
       
    }
    
    private void shutdown(int mode) {
        mh.getStandardOutput().println("\nNow shutting down...");
        Iterator it = VMManagerFactory.getInstance().machineList();
        int mustdo = vmid2ninfo.size(), done = 0;
        while(it.hasNext()){
            VirtualMachineManager vmm = (VirtualMachineManager)it.next();
            mh.getStandardOutput().print(((mode == Token.EXIT)?"Severing connection with ":"Terminating JVM ") + vmm.getName() + "... ");
            try{
                VirtualMachine vm = vmm.virtualMachine();
                
                if(mode == Token.EXIT)
                    vm.dispose();
                else vm.exit(0);
                
                mh.getStandardOutput().print("done");
                done++;
            }catch(VMDisconnectedException ex){
                mh.getStandardOutput().print("wasn't connected");
                done++;
            }catch(Exception e){
                mh.getStandardOutput().print("failed. Reason - " + e.toString());
            }
            
            mh.getStandardOutput().println(" [" + done + "/" + mustdo + "].");
        }
        mh.getStandardOutput().print("Shutdown sequence ");
        if(mustdo != done)
            mh.getStandardOutput().println("not completed successfully.");
        else
            mh.getStandardOutput().println("completed successfully.");
        
        running = false;
    }
    
    private void listGlobalThreads() {

        VMManagerFactory vmmf = VMManagerFactory.getInstance();
        DistributedThreadManager dtm = Main.getDTM();
        mh.getStandardOutput().println("Now halting DTM...");

        /* Starts by locking the DTM */
        try {
            dtm.beginSnapshot();
            mh.getStandardOutput().println(
                    "System-wide distributed thread list - \n" +
                    "       only registered threads will be shown:");

            /* First the regular threads. */
            Iterator it = vmmf.machineList();
            while (it.hasNext()) {
                VirtualMachineManager vmm = (VirtualMachineManager) it.next();
                IVMThreadManager tm;
                try{
                    tm = vmm.getThreadManager();
                }catch(VMDisconnectedException ex){
                    continue;
                }
                if (!tm.isVMSuspended())
                    mh
                            .getStandardOutput()
                            .println(
                                    "Warning - one or more Virtual Machines haven't been stopped. "
                                            + "Snapshot might not be consistent (TODO list).");
                List tlist = tm.getThreadIDList();
                Iterator tit = tlist.iterator();
                while (tit.hasNext()) {
                    Integer lt_uuid = (Integer) tit.next();
                    /*
                     * If this thread is part of a distributed thread, forget
                     * it. This is ugly but is better than confusing the user with 
                     * duplicate displays of the same thread.
                     */
                    if (dtm.getEnclosingDT(lt_uuid) != null)
                        continue;
                    String infoBase = "<" + vmm.getName() + ">" + " ["
                            + ct.uuid2Dotted(lt_uuid.intValue()) + "] ";
                    try {
                        infoBase += threadInfo(tm.findThreadByUUID(lt_uuid));
                    } catch (Exception e) {
                        infoBase += "- error: " + e.toString();
                    }
                    mh.getStandardOutput().println(infoBase);
                }
            }

            /* Now the truly distributed threads */
            it = dtm.getThreadIDList();
            while(it.hasNext()){
                Integer dt_uuid = (Integer)it.next();
                DistributedThread dt = (DistributedThread)dtm.getByUUID(dt_uuid);
                try{
                    dt.lock();
                    mh.getStandardOutput().println(threadInfo(dt));
                }finally{
                    dt.unlock();
                }
            }

        } finally {
            mh.getStandardOutput().println("Resuming DTM...");
            dtm.endSnapshot();
        }
    }
    
    private void listThreads(){
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(currentMachine);
        IVMThreadManager tm = vmm.getThreadManager();
        VirtualMachine vm = vmm.virtualMachine();
        ThreadGroupIterator it = new ThreadGroupIterator(vm.topLevelThreadGroups());
        ConversionTrait sh = ConversionTrait.getInstance();
        
        ThreadGroupReference last = null;

        while(it.hasNext()){
            ThreadGroupReference tgr = (ThreadGroupReference)it.next();
            if(tgr == null) continue;
            String spacing = "";
            // REMARK Highly inefficient (couldn't remember the sane way of doing this)
            for(int i = it.currentLevel(); i > 0; i--){
                spacing += "  ";
            }
            String group = ((it.currentLevel() == 1)?spacing + "+Group":spacing.substring(2) + "+-+Subgroup") + " [" + tgr.name() + "]";
            mh.getStandardOutput().println(group);
            Iterator tit = tgr.threads().iterator();
            while(tit.hasNext()){
                ThreadReference tr = (ThreadReference)tit.next();
                Integer debuggerId = tm.getThreadUUID(tr);
                String thread = spacing + "|  " + "[Thread - "
                        + sh.long2Hex(tr.uniqueID()) + " ("
                        + ((debuggerId == null)?"unregistered thread":ct.uuid2Dotted(debuggerId.intValue())) + ")] "
                        + threadInfo(tr);
                mh.getStandardOutput().println(thread);
            }
        }
    }
    
    private String threadInfo(DistributedThread dt){
        DistributedThread.VirtualStack vf = dt.virtualStack();
        StringBuffer info = new StringBuffer();
        info.append("<Global> [" + ct.uuid2Dotted(dt.getId()) + "] ");
        info.append((dt.isStepping())?"<stepping>":"<running>");
        info.append("\n");
        for(int i = vf.getFrameCount() - 1; i >= 0; i--){
            VirtualStackframe vs = vf.getFrame(i);
            info.append(" |--> ");
            try{
                VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vs.getLocalThreadNodeGID());
                info.append("<"+ vmm.getName() +"> [" + ct.uuid2Dotted(vs.getLocalThreadId().intValue()) + "] ");
                info.append(threadInfo(vmm.getThreadManager().findThreadByUUID(vs.getLocalThreadId())));
            }catch(Exception e){
                info.append(" error: " + e.getMessage());
            }
            info.append("\n");
        }
        
        return info.toString();
    }
    
    private String threadInfo(ThreadReference tr){
        StringBuffer tinfo = new StringBuffer();
        
        try{
            if(tr.isSuspended()){
                tinfo.append("<suspended at ");
                if(tr.frameCount() > 0){
                    Location loc = tr.frame(0).location();
                    tinfo.append(loc.method() + " " + ((loc.method().isNative())?"[native method]":""));
                }else{
                    tinfo.append("(empty or unavailable stack frame)");
                }
                tinfo.append(">");
            }else{
                tinfo.append("<running>");
            }
            tinfo.append(" " + ((tr.isAtBreakpoint())?"<at breakpoint>":""));
        }catch(IncompatibleThreadStateException ex){
            tinfo.append("<error>");
        }catch(ObjectCollectedException ex ){
            tinfo.append("<reported dead>");
        }
        
        return tinfo.toString();
    }
    
    private void advance() throws IOException {
        token = l.yylex();
        if (token == null)
            token = new Token(Token.EMPTY);
    }

    private String checkCurrent(String command)
    	throws CommandException, IOException
    {
        return checkCurrent(command, true);
    }
    
    private String checkCurrent(String command, boolean advance)
    	throws CommandException, IOException
    {
        if(advance) advance();
        if(token.type != Token.MACHINE_ID){
            if(currentMachine == null)
                badCommand(command);
            else
                return currentMachine;
        }else{
            return token.text.substring(1, token.text.length() - 1); 
        }
        
        // Shuts up the compiler
        return null;
    }

    private void badCommand(String s) throws CommandException {
        throw new CommandException(" Malformed command expression. Syntax: \n "
                + descriptions.getProperty(s));
    }

    /* (non-Javadoc)
     * @see ddproto1.Debugger#addNodes()
     */
    public void addNodes(NodeInfo[] info) throws IllegalStateException,
            ConfigException {
        if (running)
            throw new IllegalStateException(module
                    + " Cannot add more nodes while already running");

        // Loads the information into VMManagers.
        // REMARK This method is very important. It's here that the gap between
        // launchers and attachers is closed.
        for (int i = 0; i < info.length; i++) {
            NodeInfo ninfo = info[i];
            try {

                VMManagerFactory vmf = VMManagerFactory.getInstance();
                
                /* REMARK This would be the ideal:

                 * Assigns a GID for this node *
 
                ninfo.addAttribute("gid", true);
                ninfo.setAttribute("gid", String.valueOf(i));
                
                 * However, because of our current configuration design,
                 * we must know to which object we want to pass the gid 
                 * information on to. So we must first obtain the launcher
                 * prefix. 
                 *  
                */
                String lprefix = ninfo.getAttribute("launcher-class");
                /* Then we add the gid info under that prefix */
                ninfo.addAttribute(lprefix + ".gid");
                ninfo.setAttribute(lprefix + ".gid", String.valueOf(i));
                
                /* We also add the gid info under the "empty" prefix */
                ninfo.addAttribute("gid");
                ninfo.setAttribute("gid", String.valueOf(i));
                
                /* Now we add the global-agent IP address (our address) */
                String addressPort = System.getProperty("global.agent.address");
                ninfo.addAttribute(lprefix + ".global-agent-address");
                ninfo.setAttribute(lprefix + ".global-agent-address", addressPort);
                
                /* Connector arguments and VM arguments might differ from the XML-specified
                 * attribute names that have been read by the configurator. The translation
                 * is made by an IInfoCarrier decorator
                 */
                String curdir = System.getProperty("user.dir");
                URL info2vminfo = new URL("file://" + curdir + "/specs/ManagingTranslator.properties"); 
                VirtualMachineManager vmm = vmf.newVMManager(new AttributeTranslator(info2vminfo, ninfo));

                /*
                 * This will create filter that calls us back whenever a
                 * suspending event is produced.
                 */
                String mname = ninfo.getAttribute("name");
                Set <Integer> filters = new HashSet <Integer> ();
                filters.add(new Integer(EventRequest.SUSPEND_ALL));
                filters.add(new Integer(EventRequest.SUSPEND_EVENT_THREAD));
                
                vmid2ninfo.put(mname, ninfo);

            } catch (IllegalAttributeException e) {
                throw new InternalError(
                        module
                                + "Internal incompatibily exception. Unsupported attribute.");
            } catch(Exception e){
                throw new InternalError(
                        module
                                + "Internal exception -" + e.toString());
                
            }
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IEventProcessor#setNext(ddproto1.debugger.eventhandler.processors.IEventProcessor)
     */
    public void setNext(IJDIEventProcessor iep) { throw new UnsupportedException(); }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IEventProcessor#getNext()
     */
    public IJDIEventProcessor getNext() { throw new UnsupportedException(); }

    /* (non-Javadoc)
     * @see ddproto1.debugger.eventhandler.processors.IEventProcessor#enable(boolean)
     */
    public void enable(boolean status) { throw new UnsupportedException(); }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.IUICallback#queryIsRemoteOn(int)
     */
    public boolean queryIsRemoteOn(int uuid) {
        return true;
    }

    /* (non-Javadoc)
     * @see ddproto1.IDebugger#getUICallback()
     */
    public IUICallback getUICallback() {
        return this;
    }
    
    
    private void addDeferredRequestFor(String name, IDeferrableRequest req){
        List l;
        if(vmid2requests.containsKey(name)){
            l = (List)vmid2requests.get(name);
        }else{
            l = new LinkedList();
        }
        
        l.add(req);
    }
        
    private List eventRequests(String name) 
    	throws NoSuchSymbolException{
        if(!vmid2requests.containsKey(name))
            throw new NoSuchSymbolException("No requests registered for machine " + name);
        
        return((List)vmid2requests.get(name));
    }
}
