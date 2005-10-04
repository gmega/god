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
import java.util.ArrayList;
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
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.StepRequest;
import com.sun.tools.example.debug.expr.ExpressionParser;
import com.sun.tools.example.debug.expr.ParseException;
import com.sun.tools.example.debug.expr.ExpressionParser.GetFrame;
import com.sun.tools.example.debug.tty.MessageOutput;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IServiceLocator;
import ddproto1.debugger.auto.DeadlockDetector;
import ddproto1.debugger.eventhandler.processors.ApplicationExceptionDetector;
import ddproto1.debugger.eventhandler.processors.IApplicationExceptionListener;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.managing.tracker.IRealFrame;
import ddproto1.debugger.managing.tracker.VirtualStackframe;
import ddproto1.debugger.request.DeferrableBreakpointRequest;
import ddproto1.debugger.request.DeferrableExceptionRequest;
import ddproto1.exception.ConfigException;
import ddproto1.exception.InternalError;
import ddproto1.exception.CommandException;
import ddproto1.exception.InvalidStateException;
import ddproto1.exception.NoSuchElementError;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.interfaces.IUICallback;
import ddproto1.launcher.IApplicationLauncher;
import ddproto1.launcher.JVMShellLauncher;
import ddproto1.lexer.Lexer;
import ddproto1.lexer.Token;
import ddproto1.sourcemapper.ISource;
import ddproto1.sourcemapper.ISourceMapper;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.collection.ThreadGroupIterator;
import ddproto1.util.traits.JDIMiscTrait;
import ddproto1.util.traits.commons.ConversionTrait;
import ddproto1.debugger.managing.tracker.DistributedThread.VirtualStack;

/**
 * This highly inflated class represents our first attempt at constructing
 * a user interface to the DD API. It serves as a text interface in a GDB/JDB
 * style from where one can type in commands and observe results. 
 * 
 * @author giuliano
 *
 */
public class ConsoleDebugger implements IDebugger, IUICallback, IApplicationExceptionListener{

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
    private boolean running;
    
    private Map <String, Node> vmid2ninfo = new HashMap <String, Node> ();
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
            } catch (Throwable e) {
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

            if(currentMachine.equals("Global")){
                if(token.type == Token.DOTTED_ID)
                    commandResumeDT(token.text);
                break;
            }
            
            machine = checkCurrent("command.resume", false);
        
        	if(token.type == Token.MACHINE_ID) advance();
            
            if(!(token.type == Token.HEX_ID))
        	    badCommand("command.resume");
        	
        	commandResume(machine, token.text);
        	
        	break;
        	
        /**
         * Command SUSPEND - Suspends a local thread.
         *  
         * suspend (machine_id)? thread_id
         * suspend distributed_thread_dotted_id -> Global mode only.
         */
        case Token.SUSPEND:
            
            if(!currentMachine.equals("Global")){
                machine = checkCurrent("command.suspend");
        
                if(token.type == Token.MACHINE_ID) advance();
            
                if(!(token.type == Token.HEX_ID))
                    badCommand("command.suspend");
        	
                commandSuspend(machine, token.text);
            }else{
                advance();
                if(token.type == Token.DOTTED_ID){
                    commandSuspendDT(token.text);
                }else{
                    badCommand("command.suspend.global");
                }
            }
            break;

        /**
         * Command EXIT - Exits the debugger. User must specify whether we should
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
         * show radius? ("stack"|"output")? (machine_id)? (thread_id)? stackframe_number?  
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
            }else if(token.type == Token.WORD && token.text.equals("output")){
                advance();
                int tail = SHOW_DEFAULT;
                if(token.type == Token.NUMBER){ 
                    tail = Integer.parseInt(token.text);
                    advance();
                }
                machine = checkCurrent("command.show", false);
                commandShowOutput(machine, tail);
                break;
            }
            
            int frameNumber = 0;
            
            if(!currentMachine.equals("Global")){
                machine = checkCurrent("command.show", false);

                if (token.type == Token.MACHINE_ID)
                    advance();

                if (token.type == Token.HEX_ID){
                    hexy = token.text;
                    advance();
                }
                            
                if(token.type == Token.NUMBER){
                    frameNumber = Integer.parseInt(token.text);
                    advance();
                }
                
                
            	if (!(token.type == Token.EMPTY))
                	badCommand("command.show");

            	if(!stack) commandShow(machine, radius, hexy, frameNumber);
            	else commandShowStackLT(machine, hexy);

            }else{
                String dottedId = null;
                if(token.type != Token.DOTTED_ID){
                    badCommand("command.show");
                }else{
                    dottedId = token.text;
                    advance();
                }
                            
                if(token.type == Token.NUMBER){
                    frameNumber = Integer.parseInt(token.text);
                    advance();
                }
                
                if(!stack) commandShowDT(dottedId, radius, frameNumber);
                else commandShowStackDT(dottedId);
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
         * Command EVAL - Evaluates a very simple java expression and dumps its
         *                result in a rather raw form. 
         *                
         * eval framenumber (hex_id|dotted_id) java_expression
         */
        case Token.EVAL:{
            if(!currentMachine.equals("Global"))
                machine = checkCurrent("command.eval");
            else{
                machine = currentMachine;
                advance();
            }

            int frameNumber;
            
            if(token.type != Token.NUMBER)
                badCommand("command.eval");
            
            frameNumber = Integer.parseInt(token.text);
            
            advance();
            
            Value value = null;
            int exprIdx = currentCommand.lastIndexOf(" ");
            String expression = currentCommand.substring(exprIdx+1, currentCommand.length());
            
            if(token.type == Token.HEX_ID){
                if(machine.equals("Global")) throw new CommandException("Only dotted ids while in global mode.");
                value = commandEvalExpression(expression,machine,token.text, frameNumber);
            }else if(token.type == Token.DOTTED_ID){
                if(!machine.equals("Global")) throw new CommandException("Only hex ids while outside of global mode.");
                value = commandEvalExpressionDT(expression, token.text, frameNumber);
            }else{
                badCommand("command.eval");
            }

            if(value == null){
                mh.getStandardOutput().println("Expression " + expression + " value is null.");
            }else if((value instanceof ObjectReference) && !(value instanceof StringReference)){
                ObjectReference obj = (ObjectReference) value;
                this.dump(obj, obj.referenceType(), obj.referenceType());
            }else {
                String strVal = getStringValue();
                if (strVal != null) {
                    mh.getStandardOutput().println("expr is value " + value.toString());
                }
            }
            
            break;
        }
        
        /**
         * Command DETECT - Controls detectors and fires detection algorithms.
         * 
         * detect (("deadlock" (thread_list|"all")) |("application-exceptions" "on"|"off" machine_id?))
         */
        case Token.DETECT:
        {
            advance();
            if(token.type == Token.WORD && token.text.equals("deadlock")){
                advance();
                if(token.type == Token.ALL){
                    DistributedThreadManager dtm = Main.getDTM();
                    mh.getStandardOutput().println("Warning - this is still pretty experimental.");
                    try{
                        mh.getStandardOutput().print("Locking DTM...");
                        dtm.beginSnapshot();
                        mh.getStandardOutput().println("[ok]");
                        commandDetectDeadlock(null);
                        break;
                    }finally{
                        mh.getStandardOutput().print("Unlocking DTM...");
                        dtm.endSnapshot();
                        mh.getStandardOutput().println("[ok]");
                    }
                    
                }
                List<String> dtIds = new ArrayList<String>();
                while(token.type != Token.EMPTY){
                    if(token.type == Token.DOTTED_ID)
                        dtIds.add(token.text);
                    else{
                        if(token.text.equals(",")) continue;
                        mh.getErrorOutput().println("Sorry, this type of id is unsupported.");
                    }
                    advance();
                }
                commandDetectDeadlock(dtIds);
            }else if(token.type == Token.WORD && token.text.equals("applicationExceptions")){
                advance();
                String mode = token.text; 
                machine = this.checkCurrent("command.detect.application-exceptions");
                Node node = vmid2ninfo.get(machine);
                
                boolean to;
                if(mode.equals("on")) to = true;
                else if(mode.equals("off")) to = false;
                else throw new CommandException("Unrecognized toggling mode " + mode);
                
                if(node.detector == null){
                    mh.getStandardOutput().println("Set deferred application-level exception detection request to '" + mode + "'");
                    node.enableDetector = to;
                }else{
                    mh.getStandardOutput().println("Toggled application-level exception detection request '" + mode + "'");
                    node.detector.setNotificationEnabled(to);
                }
                                
            }
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
    
    private String getStringValue()
        throws CommandException
    {
        Value val = null;
        String valStr = null;
        try {
            val = ExpressionParser.getMassagedValue();
            valStr = val.toString();
        } catch (ParseException e) {
            mh.printStackTrace(e);
            throw new CommandException(e);
        }
        return valStr;
    }
    
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

        try{
            Node node = vmid2ninfo.get(vmid);
            String applicationClasses = node.spec.getAttribute(IConfigurationConstants.SKELETON_LIST);
            if(applicationClasses.length() > 0){
                ApplicationExceptionDetector aed = new ApplicationExceptionDetector(Main.getDTM());
                for(String appClass : applicationClasses.split(IConfigurationConstants.LIST_SEPARATOR_CHAR)){
                    aed.addApplicationClass(appClass);
                    List <String>appClassList = new LinkedList<String>();
                    appClassList.add(appClass);
                    DeferrableExceptionRequest der = new DeferrableExceptionRequest(
                            vmid, "java.lang.Throwable", appClassList, true,
                            true);
                    der.setProperty(ApplicationExceptionDetector.class, new Object());
                    vmm.getDeferrableRequestQueue().addEagerlyResolve(der);
                }
                node.detector = aed;
                aed.addApplicationExceptionListener(this);
                aed.setNotificationEnabled(node.enableDetector);
                vmm.setApplicationExceptionDetector(aed);
            }
                        
        }catch(AttributeAccessException ex) { 
        }catch(Exception ex){
            throw new CommandException("Failed to place exception requests. Attach failed.", ex);
        }
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

        Node node = vmid2ninfo.get(vmid);
        IObjectSpec ninfo = node.spec;

        // Checks to see if all required attributes have been set (not
        // giving a damn about their consistency).
        ninfo.validate();
        
        try{
            IServiceLocator locator = (IServiceLocator)Lookup.serviceRegistry().locate("service locator");
            IObjectSpec launcherSpec = ninfo.getChildSupporting(IApplicationLauncher.class);
            JVMShellLauncher l = (JVMShellLauncher)locator.incarnate(launcherSpec);
            node.launcher = l;
            l.launch();
        }catch(Exception e){
            throw new CommandException(e);
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

            mh
                    .getStandardOutput()
                    .println(
                            "The following JVM nodes have been configured (but not necessarily launched):");
            int i = 0;
            for (VirtualMachineManager vmm : vmmf.machineList()) {
                i++;
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
//        jmt.clearPreviousStepRequests(target, vmm);
        
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
    
    private void commandShowDT(String dottedid, int radius, int frame)
        throws CommandException
    {
        /** Gets the distributed thread. */
        DistributedThread dt = Main.getDTM().getByUUID(ct.dotted2Uuid(dottedid));
        
        if(dt == null) throw new CommandException("Invalid distributed thread - " + dottedid);
        
        VirtualStack stack = dt.virtualStack();
        int count = stack.getDTRealFrameCount();
        if(frame >= count) throw new CommandException("Invalid frame " + frame + ".");

        /** Maps the frame index to a frame index in a real thread. */
        IRealFrame rf = stack.mapToRealFrame(frame);
        int lt_uuid = rf.getLocalThreadUUID();
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(ct.guidFromUUID(lt_uuid));
        ThreadReference theThread = vmm.getThreadManager().findThreadByUUID(lt_uuid);
        
        /** This thread may not have been suspended. */
        boolean resumeAfterwards = false;
        if(!theThread.isSuspended()){
            theThread.suspend();
            resumeAfterwards = true;
        }
        StackFrame realFrame = this.getFrame(theThread, rf.getFrame(), lt_uuid);
        /** Prints the source at that location. */
        this.printSourceAtLocation(realFrame.location(), radius, vmm);
        if(resumeAfterwards) theThread.resume();
    }
    
    private StackFrame getFrame(ThreadReference tr, int frame, int ltid) throws CommandException{
        try{
            if(tr.frameCount() <= 0){
                throw new CommandException("Local thread " + ct.uuid2Dotted(ltid) + " hasn't got a valid call stack.");
            }else if(frame >= tr.frameCount()){
                throw new CommandException("Invalid stack frame " + frame);
            }
            return tr.frame(frame);
        }catch(IncompatibleThreadStateException ex){
            throw new CommandException("Thread " + ct.uuid2Dotted(ltid) + " hasn't been suspended.", ex);
        }
    }
    
    private void commandShowStackDT(String dottedid) 
    	throws CommandException
    {
        try {

            StringBuffer outInfo = new StringBuffer();
            outInfo.append("Stack information for distributed thread " + "[" + dottedid + "]:\n");
            outInfo.append(renderDTStack(dottedid));
            mh.getStandardOutput().println(outInfo.toString());
            
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }
    
    private String renderDTStack(String dottedid)
        throws IncompatibleThreadStateException
    {
        Integer dt_uuid = new Integer(ct.dotted2Uuid(dottedid));
        DistributedThreadManager dtm = Main.getDTM();

        DistributedThread dt = dtm.getByUUID(dt_uuid);
        assert (dt != null);
        VirtualStack vs = dt.virtualStack();

        int acc = 0;
        
        List<DisassembledStackFrame> frames = new ArrayList<DisassembledStackFrame>();
        for (int i = 0; i < vs.getVirtualFrameCount(); i++) {
            VirtualStackframe vf = vs.getVirtualFrame(i);
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
            int base;
            int top;
            
            if(i == (vs.getVirtualFrameCount() - 1)) base = 1;
            else base = callBase.intValue();
            
            if(i == 0) top = tr.frameCount();
            else top = callTop.intValue();
            
            frames.addAll(threadStack(tr, base, top, acc, vmm.getName(), true));
            acc += top - base + 1;
            
            if(doResume) tr.resume();
        }
        
        return assembleStackFrames(frames);
    }
    
    private void commandShow(String machine, int radius, String hexid, int frame)
    	throws CommandException, IOException
    {
   
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        ThreadReference target = null;
        
        target = grabSuspendedThread(hexid, vmm);
        StackFrame sf = this.getFrame(target, frame, ct.hex2Int(hexid));
        Location loc = sf.location();
        this.printSourceAtLocation(loc, radius, vmm);
    }
    
    private void printSourceAtLocation(Location loc, int radius, VirtualMachineManager vmm)
        throws CommandException {
        try {

            ISourceMapper sm = vmm.getSourceMapper();
            ISource is = sm.getSource(loc);
            int line = loc.lineNumber();
            int i = (line - radius) > 0 ? line - radius : 0;

            for (; i < (line + radius); i++) {
                String linestr = "[" + i + "]: ";
                if (i == line)
                    linestr = "[" + i + "]=>";
                mh.getStandardOutput().println(linestr + is.getLine(i));
            }
        } catch (AbsentInformationException e) {
            throw new CommandException(
                    "No valid source-code information found.");
        } catch (IOException ex) {
            throw new CommandException("Error reading source-code file.");
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
            
            List<DisassembledStackFrame> frames = threadStack(target, 1, target.frameCount(), 0, null, false);
            
            mh.getStandardOutput().println(assembleStackFrames(frames));
            
        }catch(IncompatibleThreadStateException e){
            throw new CommandException(e);
        }
    }
    
    private void commandShowOutput(String machine, int lines)
        throws CommandException
    {
        Node theNode = vmid2ninfo.get(machine);
        assert theNode != null;
        mh.getStandardOutput().println(
                "Last " + lines + " of output produced by <" + machine + ">");
        try{
            List <String> oup = theNode.launcher.getShellTunnel().getStdoutResult();
            int size = oup.size();
            int until = Math.min(size, lines);
            for(int k = (size - until); k < size; k++)
                mh.getStandardOutput().println(oup.get(k));
            
        }catch(InvalidStateException ex){
            throw new CommandException(ex);
        }
    }
    
    private String assembleStackFrames(List<DisassembledStackFrame> sf){
                
        StringBuffer pr = new StringBuffer();
        
        /** Preprocesses all data for padding. */
        int [] maxes = new int[STRING_INFO];
                
        for(int i = 0; i < STRING_INFO; i++){
            maxes[i] = 0;
            for(DisassembledStackFrame dsf : sf ){
            	if(dsf.strData[i] == null) continue;
                if(maxes[i] < dsf.strData[i].length()) 
                    maxes[i] = dsf.strData[i].length();
            }
        }
        
        for(DisassembledStackFrame dsf : sf){
            for(int i = 0; i < STRING_INFO; i++){
                pr.append(stackRenderers[i].renderColumn(dsf.strData[i], maxes[i], dsf.booData));
            }
            pr.append("\n");
        }
        
        return pr.toString();
    }

    private List<DisassembledStackFrame> threadStack(ThreadReference target, int base, int top, int begin, String machine, boolean showMonitors)
    	throws IncompatibleThreadStateException
    {
        
        base = Math.max(0, target.frameCount() - base);
        top = Math.max(0, target.frameCount() - top);
        
        assert(top <= base);
        
        List<DisassembledStackFrame> stackframeInfo = new ArrayList<DisassembledStackFrame>();
        
        for(int i = top; i <= base; i++, begin++){
            DisassembledStackFrame dsf = new DisassembledStackFrame();
            StringBuffer pr = new StringBuffer();
            dsf.strData[MACHINE_NAME] = (machine != null)?machine.trim():null;
            dsf.booData[TOP_LINE] = i == top;
            dsf.booData[BOTTOM_LINE] = i == base;
            dsf.booData[MIDDLE_LINE] = (i == (top+base)/2);
            
            StackFrame currentFrame = target.frame(i);
            Location loc = currentFrame.location();
            String lineNumber = "<unknown line number>";
            String source = "<unknown source>";
            try{
                lineNumber = Integer.toString(loc.lineNumber());
                source = loc.sourceName();
            }catch(AbsentInformationException ex){ }
                        
            Method m = loc.method();

            pr.append(begin + " " + loc.declaringType().name() + "."
                            + m.name() + "(");
            
            Iterator it = 
                m.argumentTypeNames().iterator();

            Iterator <LocalVariable>argIt = null;
            
            try{
                argIt = m.arguments().iterator();
            }catch(AbsentInformationException ex) { }
            
            while(it.hasNext()){
                String arg = (argIt != null && argIt.hasNext())?argIt.next().name():"<unknown>";
                pr.append((String)it.next() + " " + arg);
                if(it.hasNext()) pr.append(",");
            }
                        
            pr.append("):" + source + "[" + lineNumber + "]");
            
            dsf.strData[MONITOR_LIST] = "";
            
            if(showMonitors && i == top){
                String monList = monitorList(target, machine.trim());
                if(monList != null) dsf.strData[MONITOR_LIST] = monList;
            }
            
            dsf.strData[LINE] = pr.toString();
            
            stackframeInfo.add(dsf);
 
        }
        return stackframeInfo;
    }
    
    private String monitorList(ThreadReference target, String machine)
        throws IncompatibleThreadStateException
    {
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(machine);
        String gid = vmm.getGID();
        
        StringBuffer monitorList = new StringBuffer();
        
        for(ObjectReference monitor : target.ownedMonitors()){
            monitorList.append(gid);
            monitorList.append(".");
            monitorList.append(ct.long2Hex(monitor.uniqueID()));
            monitorList.append(",");
        }
        
        String wQual = "<unknown>";
        
        if(target.status() == ThreadReference.THREAD_STATUS_MONITOR){
            wQual = "<blocked>";
        }else if(target.status() == ThreadReference.THREAD_STATUS_WAIT){
            wQual = "<waiting>";
        }
        
        ObjectReference contended = target.currentContendedMonitor();
        if(contended != null)
            monitorList.append(wQual + gid+"."+ct.long2Hex(contended.uniqueID()));
        else
            if(monitorList.length() > 0) monitorList.deleteCharAt(monitorList.length()-1);
        
        return monitorList.toString();
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
    
    private void commandDetectDeadlock(List<String> dtIds)
        throws CommandException
    {
        DistributedThreadManager dtm = Main.getDTM();
        Iterator<Integer> idList;
                
        if(dtIds == null){
            idList = dtm.getThreadIDList();
        }else{
            List<Integer> dtIntegerIds = new ArrayList<Integer>();
            for(String dtId : dtIds) dtIntegerIds.add(ct.dotted2Uuid(dtId));
            idList = dtIntegerIds.iterator();
        }
                
        Set<DistributedThread> dtSet = new HashSet<DistributedThread>();
        while(idList.hasNext()){
            int uuid = idList.next();
            DistributedThread dt = dtm.getByUUID(uuid);
            dtSet.add(dt);
        }
        DeadlockDetector detector = new DeadlockDetector();
        detector.detect(dtSet, new HashSet<ThreadReference>());
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
    
    private void commandSuspendDT(String dottedId){
        DistributedThreadManager dtm = Main.getDTM();
        DistributedThread dt = dtm.getByUUID(ct.dotted2Uuid(dottedId));
        try{
            dt.lock();
            dt.suspend();
        }finally{
            dt.unlock();
        }
        mh.getStandardOutput().println("Distributed thread " + dottedId + " suspended." +
                "\n Please resume it through global mode or things go bad. ");
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
    
    private Value commandEvalExpressionDT(String expression, String dotted, int frame)
        throws CommandException
    {
        Value result = null;
        ExpressionParser.GetFrame frameGetter = null;

        DistributedThreadManager dtm = Main.getDTM();
        DistributedThread dt = dtm.getByUUID(ct.dotted2Uuid(dotted));
        try{
            dt.lock();
            if(!dt.isSuspended()) throw new CommandException("You must suspend thread " + dotted + " first.");
        }finally{
            dt.unlock();
        }
        VirtualStack vs = dt.virtualStack();
        
        if(vs.getDTRealFrameCount() <= frame) throw new CommandException("Invalid frame.");
        IRealFrame rf = vs.mapToRealFrame(frame);
        VirtualMachineManager vmm = VMManagerFactory.getInstance()
                .getVMManager(ct.guidFromUUID(rf.getLocalThreadUUID()));
        final ThreadReference tr = vmm.getThreadManager().findThreadByUUID(rf.getLocalThreadUUID());
        boolean resume = false;
        try{
            while(!tr.isSuspended()){ tr.suspend(); resume = true; }
            final int _frame = rf.getFrame();
            frameGetter = new ExpressionParser.GetFrame() {
                public StackFrame get()
                    throws IncompatibleThreadStateException {
                        return tr.frame(_frame);
                    }
            };
        
            return doGetValue(expression, frameGetter, tr.virtualMachine());
        }finally{
            if(resume && tr.isSuspended()) tr.resume();
        }
    }
    
    private Value commandEvalExpression(String expression, String vmid, String hexy, int frame)
        throws CommandException
    {
        Value result = null;
        ExpressionParser.GetFrame frameGetter = null;
        
        VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vmid);
        
        final ThreadReference tr = this.grabSuspendedThread(hexy, vmm);
        final int _frame = frame;
        
        frameGetter = new ExpressionParser.GetFrame() {
            public StackFrame get()
                throws IncompatibleThreadStateException {
                    return tr.frame(_frame);
                }
        };

        try {
            if(tr.frameCount() <= frame) throw new CommandException("Invalid frame.");
        } catch (IncompatibleThreadStateException e) {
            throw new CommandException(e);
        }

        return this.doGetValue(expression, frameGetter, tr.virtualMachine());
    }
    
    private Value doGetValue(String expression, GetFrame gt, VirtualMachine vm) 
        throws CommandException
    {
        try {
            return ExpressionParser.evaluate(expression,
                    vm, gt);
        } catch (InvocationException ie) {
            mh.getErrorOutput().println(
                    "Exception in expression:"
                            + ie.exception().referenceType().name());
            throw new CommandException(ie);
        } catch (Exception ex) {
            mh.printStackTrace(ex);
            throw new CommandException(ex);
        }
    }
    
    public void exceptionOccurred(String cause, ObjectReference exception, int dt_uuid, int lt_uuid){
        MessageHandler mh = MessageHandler.getInstance();
        StringBuffer exceptionReport = new StringBuffer();
        exceptionReport.append("- Application-level exception detected."
                                + "\n- Distributed thread id: " + ct.uuid2Dotted(dt_uuid)
                                + "\n- Local thread globally unique id: " + ct.uuid2Dotted(lt_uuid)
                                + "\n- Exception is: " + exception.referenceType().name()  
                                + "\n- Reported cause is :" + cause
                                + "\n- Distributed Stack trace: \n");
        
        try{
            exceptionReport.append(this.renderDTStack(ct.uuid2Dotted(dt_uuid)));
            mh.getStandardOutput().println(exceptionReport.toString());
        }catch(Exception ex){
            mh.getStandardOutput().println(
                            exceptionReport.toString()
                                    + "\n Sorry, there was an error while acquiring the stack trace.");
            mh.printStackTrace(ex);
        }
    }
    
    private void dump(ObjectReference obj, ReferenceType refType,
            ReferenceType refTypeBase) {
        for (Iterator it = refType.fields().iterator(); it.hasNext();) {
            StringBuffer o = new StringBuffer();
            Field field = (Field) it.next();
            o.append("    ");
            if (!refType.equals(refTypeBase)) {
                o.append(refType.name());
                o.append(".");
            }
            o.append(field.name());
            o.append(": ");
            o.append(obj.getValue(field));
            mh.getStandardOutput().println(o.toString()); // Special case: use printDirectln()
        }
        if (refType instanceof ClassType) {
            ClassType sup = ((ClassType) refType).superclass();
            if (sup != null) {
                dump(obj, sup, refTypeBase);
            }
        } else if (refType instanceof InterfaceType) {
            List sups = ((InterfaceType) refType).superinterfaces();
            for (Iterator it = sups.iterator(); it.hasNext();) {
                dump(obj, (ReferenceType) it.next(), refTypeBase);
            }
        } else {
            /* else refType is an instanceof ArrayType */
            if (obj instanceof ArrayReference) {
                mh.getStandardOutput().print("[");
                for (Iterator it = ((ArrayReference) obj).getValues().iterator(); it.hasNext();) {
                    mh.getStandardOutput().print(it.next().toString());// Special case: use printDirect()
                    if (it.hasNext()) {
                        mh.getStandardOutput().print(", ");// Special case: use printDirect()
                    }
                }
                mh.getStandardOutput().println("]");
            }
        }
    }
    
    private ThreadReference grabSuspendedThread(String hexy, VirtualMachineManager vmm)
    	throws CommandException
    {
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
        int mustdo = vmid2ninfo.size(), done = 0;
        for(VirtualMachineManager vmm : VMManagerFactory.getInstance().machineList()){
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
        boolean firstUnstopped = true;

        /* Starts by locking the DTM */
        try {
            dtm.beginSnapshot();
            mh.getStandardOutput().println(
                    "System-wide distributed thread list - \n" +
                    "       only registered threads will be shown:");

            /* First the regular threads. */
            for(VirtualMachineManager vmm : vmmf.machineList()) {
                IVMThreadManager tm;
                try{
                    tm = vmm.getThreadManager();
                }catch(VMDisconnectedException ex){
                    continue;
                }
                if (!tm.isVMSuspended() && !firstUnstopped){
                    mh.getStandardOutput().println(
                            "Warning - one or more Virtual Machines haven't been stopped. "
                                            + "Snapshot might not be consistent (TODO list).");
                    firstUnstopped = true;
                }
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
            Iterator it = dtm.getThreadIDList();
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
        
        while(it.hasNext()){
            ThreadGroupReference tgr = (ThreadGroupReference)it.next();
            if(tgr == null) continue;
            StringBuffer spacing = new StringBuffer();
            for(int i = it.currentLevel(); i > 0; i--){
                spacing.append("  ");
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
        for(int i = vf.getVirtualFrameCount() - 1; i >= 0; i--){
            VirtualStackframe vs = vf.getVirtualFrame(i);
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
        
        String machine = null;
        
        if(token.type != Token.MACHINE_ID){
            if(currentMachine == null)
                badCommand(command);
            else
                machine = currentMachine;
        }else{
            machine = token.text.substring(1, token.text.length() - 1); 
        }
        
        if(!vmid2ninfo.containsKey(machine))
            throw new CommandException("Unknown machine <" + machine +">");
        
        return machine;
    }

    private void badCommand(String s) throws CommandException {
        throw new CommandException(" Malformed command expression. Syntax: \n "
                + descriptions.getProperty(s));
    }

    /* (non-Javadoc)
     * @see ddproto1.Debugger#addNodes()
     */
    public void addNodes(List<IObjectSpec> nodeList) throws IllegalStateException,
            ConfigException {
        if (running)
            throw new IllegalStateException(module
                    + " Cannot add more nodes while already running");

        // Loads the information into VMManagers.
        // REMARK This method is very important. It's here that the gap between
        // launchers and attachers is closed.
        for (IObjectSpec node : nodeList) {
            try {
                String mname = node.getAttribute(IConfigurationConstants.NAME_ATTRIB);
                Node nodeStruct = new Node();
                nodeStruct.spec = node;
                vmid2ninfo.put(mname, nodeStruct);
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

    public boolean queryIsRemoteOn(int uuid) {
        return true;
    }

    /* (non-Javadoc)
     * @see ddproto1.IDebugger#getUICallback()
     */
    public IUICallback getUICallback() {
        return this;
    }
    
    private class Node{
        private IObjectSpec spec;
        private JVMShellLauncher launcher;
        private ApplicationExceptionDetector detector;
        boolean enableDetector = false;
    }
    
    /** Printing stuff */
    public static int MACHINE_NAME = 0; // First Column
    public static int LINE = 1;         // Second Column
    public static int MONITOR_LIST = 2; // Third Column
    public static int STRING_INFO = 3;
    
    public static int MIDDLE_LINE = 0;
    public static int TOP_LINE = 1;
    public static int BOTTOM_LINE = 2;
    public static int BOOLEAN_INFO = 3;

    private class DisassembledStackFrame{
        public String [] strData = new String[STRING_INFO];
        public boolean [] booData = new boolean[BOOLEAN_INFO];
    }
    
    private ColumnRenderer [] stackRenderers = new ColumnRenderer[]{
        new MachineRenderer(), new StackFrameLineRenderer(), new MonitorListRenderer()
    };
    
    private interface ColumnRenderer{
        public String renderColumn(String data, int padding, boolean [] flags);
    }
   
    private class MachineRenderer implements ColumnRenderer{
        public String renderColumn(String data, int length, boolean[] flags) {
            
            if(data == null) return "";
            
            /* Pad the machine name */
            StringBuffer frontPadding = new StringBuffer();
            StringBuffer backPadding = new StringBuffer();
            
            length = length - data.length() + 2;
            
            for(int j = 0; j < Math.floor(length/2); j++){
                frontPadding.append(" ");
                backPadding.append(" ");
            }
            
            for(int j = 0; j < length%2; j++)
                frontPadding.append(" ");
            
            backPadding.setCharAt(0, '[');
            frontPadding.setCharAt(frontPadding.length()-1, ']');
            
            String machineName = backPadding.append(data).append(frontPadding).toString();
            
            StringBuffer line = new StringBuffer();
            
            if(flags[TOP_LINE] || flags[BOTTOM_LINE]) 
                line.append((flags[MIDDLE_LINE]?machineName+"<---":machineName.replaceAll("."," ") + "    ") + "+---->");
            else 
                line.append( flags[MIDDLE_LINE]?(machineName+"<---|     "):(machineName.replaceAll("."," ") + "    |     "));
            
            return line.toString();
        }
    }
    
    private class StackFrameLineRenderer implements ColumnRenderer{
        public String renderColumn(String data, int padding, boolean[] flags) {
            StringBuffer line = new StringBuffer();
            line.append(data);
            int length = padding - data.length();
            for(int i = 0; i <= length ; i++)
                line.append(" ");
            
            return line.toString();
        }
    }
    
    private class MonitorListRenderer implements ColumnRenderer{
        public String renderColumn(String data, int padding, boolean[] flags) {
        	StringBuffer sr = new StringBuffer();
        	sr.append("| ");
        	if(data.length() > 0){
        		sr.append("[");
        		sr.append(data);
        		sr.append("]");
        	}
        	return sr.toString(); 
        }
    }
    
}
