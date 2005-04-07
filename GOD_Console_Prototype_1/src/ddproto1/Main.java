/*
 * Created on Jul 21, 2004
 *
 * DISCLAIMER: Do not attempt to debug your airplane and/or nuclear missile 
 * silo with this software. 
 *
 */
package ddproto1;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IConfigurator;
import ddproto1.configurator.InfoCarrierWrapper;
import ddproto1.configurator.NodeInfo;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThreadManager;
import ddproto1.debugger.server.SeparatingHandler;
import ddproto1.debugger.server.SocketServer;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.interfaces.IMessageBox;
import ddproto1.interfaces.IUICallback;
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

    public static final int CONFIGURATION_SERVER_MAX_THREADS = 20;
    public static final int CONFIGURATION_SERVER_QUEUE_SIZE = 10;

    private static String configclass = "ddproto1.configurator.DefaultConfiguratorImpl";
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
            Class config = Class.forName(configclass);
            Class debug = Class.forName(debuggerclass);
            IConfigurator cfg = (IConfigurator)config.newInstance();
            IDebugger dbg = (IDebugger)debug.newInstance();
            ui = dbg.getUICallback();
            
            if(basedir == null) basedir = System.getProperty("user.dir");
            String separator = File.separator;
            if(!basedir.endsWith(separator)) basedir += separator;
            Collection nodes = cfg.parseConfig(new URL("file://" + basedir + "dd_config.xml"));
            NodeInfo [] ninfo = new NodeInfo[nodes.size()];
            nodes.toArray(ninfo);
                        
            /* Adds node info to debugger */
            dbg.addNodes(ninfo);
            
            /* Creates and adds node info to the Distributed Thread Manager */
            dtm = new DistributedThreadManager(dbg.getUICallback());
            VMManagerFactory vmmf = VMManagerFactory.getInstance();
            Iterator it = vmmf.machineList();
            while(it.hasNext())
                dtm.registerNode((VirtualMachineManager)it.next());
            
            /* Now that the Distributed Thread Mananger has been created, we can 
             * start the central server.
             */
            setServer(ninfo);
            
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
    
    private static void setServer(NodeInfo [] ninfo)
    	throws IllegalAttributeException
    {
        
        String [] address = System.getProperty("global.agent.address").split(":");
        if(address.length != 2){
            throw new IllegalAttributeException("Wrong global agent address specification format. " +
            		"Must be of the form [address]:[port]");
        }
        
        /* Sets the configuration/notification server */
        SocketServer scs = new SocketServer(Integer.parseInt(address[1])
                , CONFIGURATION_SERVER_MAX_THREADS,
                CONFIGURATION_SERVER_QUEUE_SIZE);
        
        InfoCarrierWrapper icw = new InfoCarrierWrapper(ninfo);
        SeparatingHandler byStatus = new SeparatingHandler(DebuggerConstants.STATUS_FIELD_OFFSET);
        byStatus.registerHandler(DebuggerConstants.NOTIFICATION, dtm);
        byStatus.registerHandler(DebuggerConstants.REQUEST, icw);
        
        MessageHandler mh = MessageHandler.getInstance();
        
        if(ninfo.length > 255){
            throw new InternalError("Cannot proceed - addressing design does not " +
            		"allow more than 255 nodes");
        }
            
        
        for(int i = 0; i < ninfo.length; i++){
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

                if (arg.equals("configurator-class")) {
                    configclass = spec[1];
                } else if (arg.equals("debugger-class")) {
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
        Logger l = Logger.getLogger("agent.global");
        /* TODO Allow custom configuration file for global agent logger */
        BasicConfigurator.configure();
    }
    
    private static void printUsage(){
        System.out.println("Syntax: " + progname + " --op1=val1 --op2=val2 ... ");
        System.out.println("Where the opXXX are:");
        System.out.println("   -  configurator-class: specifies a class to use");
        System.out.println("      as configurator for this session.");
        System.out.println("      defaults to " + configclass);
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
