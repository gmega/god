/*
 * Created on Jul 21, 2004
 *
 * DISCLAIMER: Do not attempt to debug your airplane and/or nuclear missile 
 * silo with this software. 
 *
 */
package ddproto1;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.InfoCarrierWrapper;
import ddproto1.configurator.newimpl.BranchKey;
import ddproto1.configurator.newimpl.IConfigurationConstants;
import ddproto1.configurator.newimpl.IIntegerInterval;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.SpecLoader;
import ddproto1.configurator.newimpl.StandardServiceLocator;
import ddproto1.configurator.newimpl.XMLConfigurationParser;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.distributed.DefaultGUIDManager;
import ddproto1.debugger.managing.distributed.IGUIDManager;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.server.SeparatingHandler;
import ddproto1.debugger.server.SocketServer;
import ddproto1.exception.AttributeAccessException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.ResourceLimitReachedException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.interfaces.IUICallback;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;

/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Main {

    public static final String progname = "dd";
    public static final String module = "Main -";
    
    public final static String app = "Distributed Debugger Prototype 1";

    public static final String DD_CONFIG_FILENAME = "dd_config.xml";

    private static String debuggerclass = "ddproto1.ConsoleDebugger";
    private static String basedir =  null;
    private static IUICallback ui = null;
    
    /* REMARK This attribute confuses the semantics of the Main class (which is not
     * a class at all) with the semantics of the (yet inexistent) DistributedSystem 
     * class. 
     * That confusion happens mainly because this attribute gets kind of
     * lost in the middle of nowhere, but since this is a prototype anyway, why not 
     * just let it as it is for now? 
     */
    private static DistributedThreadManager dtm;
        
    public static void main(String[] args) {
        System.out.println("GOD Prototype 1 0.10 - Copyleft (c) Giuliano Mega, 2004. No rights reserved.\n" +
        		           "And remember: This software doesn't work.\n");
        
        // Parses parameters and sets I/O functions
        parseParameters(args);
        setIO();
        
        // Loads classes, launches remote configuration server and debugger
        try{
            /** Loads the custom user interface class */
            Class debug = Class.forName(debuggerclass);
            IDebugger dbg = (IDebugger)debug.newInstance();
            ui = dbg.getUICallback();
            
            /** Acquires the base directory of the running program */
            if(basedir == null) basedir = System.getProperty("user.dir");
            String separator = File.separator;
            if(!basedir.endsWith(separator)) basedir += separator;

            /** Creates a new XML configuration parser, assuming that all constraint
             * specs are located in basedir/SPECS_DIR, including the TOC.
             */
            String url = "file://" + basedir + IConfigurationConstants.SPECS_DIR;
            List <String> specPath = new ArrayList<String>();
            specPath.add(url);
            XMLConfigurationParser cfg = new XMLConfigurationParser(new SpecLoader(specPath, url));
            
            /** Parses the configuration file */
            IObjectSpec root = cfg.parseConfig(new URL("file://" + basedir + DD_CONFIG_FILENAME)); 
            
            /** Creates and adds node info to the Distributed Thread Manager */
            dtm = new DistributedThreadManager(dbg.getUICallback());
            VMManagerFactory vmmf = VMManagerFactory.getInstance();
            Iterator it = vmmf.machineList();
            while(it.hasNext())
                dtm.registerNode((VirtualMachineManager)it.next());
            
            /* Now that the Distributed Thread Mananger has been created, we can 
             * start the central server.
             */
            setServer(root);
            
            setServices();
            
            /* Starts the debugger */
            dbg.mainLoop();
            
            System.exit(0);
            
        }catch(ClassCastException e){
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
    public static DistributedThreadManager getDTM(){
        return dtm;
    }
    
    private static void setServices() throws DuplicateSymbolException{
        Lookup.serviceRegistry().register("service locator", StandardServiceLocator.getInstance());
    }
    
    private static void setServer(IObjectSpec root)
    	throws AttributeAccessException, UnknownHostException,
            DuplicateSymbolException, ResourceLimitReachedException,
            ClassNotFoundException
    {

        /** Obtains the server configuration data. */
        String address = root.getAttribute(IConfigurationConstants.GLOBAL_AGENT_ADDRESS);
        if(address.equals(IConfigurationConstants.AUTO)) address = InetAddress.getLocalHost().getHostAddress();
        int port = Integer.parseInt(root.getAttribute(IConfigurationConstants.PORT));
        int queue_size = Integer.parseInt(root.getAttribute(IConfigurationConstants.MAX_QUEUE_LENGTH));
        
        /* Sets the configuration/notification server */
        SocketServer scs = new SocketServer(port, port, queue_size);
        
        List<IObjectSpec> nodeList = root.getChildrenOfType(IConfigurationConstants.NODE_LIST);
        
        /** We trust that the parser did the cardinality checks correctly and that there's only
         * one node list. However, asserting that this property stands correct never hurts.
         */
        assert(nodeList.size() == 1);
        
        InfoCarrierWrapper icw = new InfoCarrierWrapper(nodeList);
        SeparatingHandler byStatus = new SeparatingHandler(DebuggerConstants.STATUS_FIELD_OFFSET);
        byStatus.registerHandler(DebuggerConstants.NOTIFICATION, dtm);
        byStatus.registerHandler(DebuggerConstants.REQUEST, icw);
        
        MessageHandler mh = MessageHandler.getInstance();
        
        if(nodeList.size() > 255){
            throw new InternalError("Cannot proceed - addressing design does not " +
            		"allow more than 255 nodes");
        }
            
        assignInitialGUIDs(nodeList);
        
        for(int i = 0; i < nodeList.size(); i++){
            try{
                /* REMARK We could add an address check to make sure no one
                 * is faking its gid (or something went bad along the way).
                 */
                scs.registerNode(new Byte((byte)i), byStatus);
            }catch(ConfigException e){
                mh.printStackTrace(e);
            }
        }
        
        Thread sthread = new Thread(scs);
        sthread.start();
    }
    
    private static void assignInitialGUIDs(List<IObjectSpec> nodeList)
        throws DuplicateSymbolException, ResourceLimitReachedException, AttributeAccessException
        
    {
        IGUIDManager guidManager = new DefaultGUIDManager(256);
        
        /** Since the console version won't allow you to add/remove new nodes (for now),
         * I just toss the guid manager away once I'm done with it.
         */
        for(IObjectSpec node : nodeList){
            String guid = node.getAttribute(IConfigurationConstants.GUID);
            if(guid.equals(IConfigurationConstants.AUTO)){
                int _guid = guidManager.leaseGUID(node);
                node.setAttribute(IConfigurationConstants.GUID, Integer.toString(_guid));
            }
        }
        
        
    }
    
    private void lixo(){ }
    
    private static void parseParameters(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) printUsage();
            if (args[i].startsWith("--")) {
                String arg = args[i].substring(3);
                String[] spec = arg.split("=");

                if (!(spec.length == 2)) {
                    System.out.println("Error - wrong parameter format.");
                    printUsage();
                }
                if (arg.equals("debugger-class")) {
                    debuggerclass = spec[1];
                } else if (arg.equals("basedir")) {
                    basedir = spec[1];
                } else {
                    System.out.print("Error - invalid parameter " + spec[1]);
                    printUsage();
                }

            } else {
                System.out.println("Invalid parameter: " + args[i]);
                printUsage();
            }

        }
    }
    
    private static void setIO(){
        IMessageBox stdout = new IMessageBox(){
        
            public void println(String s){
                ui.printLine(s);
            }
            
            public void print(String s){
                ui.printMessage(s);
            }
        };
        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setErrorOutput(stdout);
        mh.setStandardOutput(stdout);
        mh.setWarningOutput(stdout);
        mh.setDebugOutput(stdout);
        
        /* This isn't very good. Some classes use the log4j logger so we
         * must set it up. 
         * REFACTORME My idea is moving everyone to the log4j Logger.
         */
        Logger.getLogger("agent.global");
        /* TODO Allow custom configuration file for global agent logger */
        BasicConfigurator.configure();
    }
    
    private static void printUsage(){
        System.out.println("Syntax: " + progname + " --op1=val1 --op2=val2 ... ");
        System.out.println("Where the opXXX are:");
        System.out.println("   -  debugger-class: specifies the debugger class");
        System.out.println("      to use for this session.");
        System.out.println("      defaults to " + debuggerclass);
        System.out.println("   -  basedir: specifies the base directory where");
        System.out.println("      the debugger should look for its configuration");
        System.out.println("      file and dtd specifications.");
        System.out.println("      defaults to " + basedir);
        
        System.exit(-1);
    }
}
