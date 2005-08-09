/*
 * Created on Aug 29, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: LocalLauncher.java
 */

package ddproto1.localagent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.util.JavaWrapper;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.IConfigurator;
import ddproto1.exception.CommException;
import ddproto1.localagent.CORBA.CORBAHook;
import ddproto1.localagent.CORBA.ORBHolder;
import ddproto1.localagent.CORBA.orbspecific.JacORBStampRetriever;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;

/**
 * This class serves as a wrapper for the debuggee JVM main class. It's 
 * responsible for delegating application classloading to our own internal
 * classloader, which will instrument required methods.  
 * 
 * The wrapper takes three required and one optional parameter. Those are:
 * 
 * --gid=XX				*Specifies the global id assigned to this JVM
 * --mainClass=name		*Specifies the main class of the debuggee application
 * --central-agent=addr	*Specifies the address:port of the central host.
 * --logger-config=file	Specifies the name of the file for configuring the
 * 						logger. Will search for "file.internal" for configuring
 * 						the internal logger.
 * 
 * Starred parameters are required.
 * 
 * @author giuliano
 *
 */
public class LocalLauncher extends JavaWrapper{
    
    private static byte gid;
    private static String mainClass;
    private static String loggerConfig;
    private static Logger logger = Logger.getLogger(LocalLauncher.class);
    
    private static IGlobalAgent globalAgent;
    
    public static void main(String[] args) 
        throws Exception
    {
       
        setLogger();
        
        logger.info("LocalAgent V0.1a - By Giuliano, 2004. \n" +
        			"Do not use this software to debug your airplane.\n");
        
      
        /* Parses parameters - the three required parameters must 
         * come first. */
        int start = 0;
        for(int i = 0; i < args.length; i++){

            String [] arg = null;
            if(!args[i].startsWith("--app-parameters")){
                arg = args[i].split("=");
                if(arg.length != 2){
                    logger.fatal("Wrong parameter format - " + args[i]);
                    printUsage();
                    System.exit(1); 
                }
            }else{
                arg = new String[]{args[i]};
            }

            if(arg[0].startsWith("--main-class")){
                mainClass = arg[1];
            } else if(arg[0].startsWith("--logger-config")){
                loggerConfig = arg[1];
            } else if(arg[0].startsWith("--app-parameters")){
                if(args.length > i + 1){
                    start = i + 1; // At least one parameter
                    break;
                }
            } else {
                logger.fatal("Invalid parameter - " + args[i]);
                printUsage();
                System.exit(1);
            }
        }
        
        gid = Byte.parseByte(System.getProperty("agent.local.gid"));
        
        if(mainClass == null){
            logger.error("Error - required parameter missing.");
            printUsage();
            System.exit(2);
        }
        
        /* If we don't let the JacORB package classes through, some weird stuff
         * starts to happen.
         * TODO Allow specification of excluded packages through configuration file.
         */ 
        java.lang.ClassLoader dl = new DebugLoader(new String[] {"org.omg", "org.jacorb", "ddproto1"});
        
        try {
            /* Creates the proxy to the global agent */
            GlobalAgentFactory gaFactory = GlobalAgentFactory.getInstance();
            globalAgent = gaFactory.resolveReference();
            
            /* Initializes global tagger
             * We have to be cautious when adopting singletons with
             * multiple classloaders. */
            Class taggerClass = dl.loadClass("ddproto1.localagent.Tagger");
            Method getInstance = taggerClass.getMethod("getInstance", null);
            Method setGID = taggerClass.getMethod("setGID", new Class[] { Byte.TYPE });
            Object tagger = getInstance.invoke(null, null);
            setGID.invoke(tagger, new Object[] { new Byte(gid) });
            
            /* Tags the main thread. Hopefully we'll be able to tag all threads. */
            Method tagCurrent = taggerClass.getMethod("tagCurrent", new Class[] {});
            tagCurrent.invoke(tagger, new Object[] { });
            
            /* Sets the agent loader - we have to do this since 
             * our loader cannot inherit from the current loader*/
            setInternalLogger(dl);
            createDefaultHooks((DebugLoader)dl);

            /* Invokes the main class */
            Class debuggee = dl.loadClass(mainClass);
            Method main = debuggee.getMethod("main",
                    new Class[] { String[].class });
            String new_args[] = null;
            if(start != -1){
                new_args = new String[args.length - start];
                System.arraycopy(args, start, new_args, 0, new_args.length);
            }else{
                new_args = new String [] {}; 
            }

            /* This is a requirement for JacORB. */
            /* REMARK Java ClassLoading is a potential source of disasters, especially
             * because some of its implications cannot be circumvented. We
             * are likely to be forced to devise some mechanism for replacing
             * the system classloader in the near future or add support for bytecode
             * enhancing outside the classloading realm ("gcc -ggdb" style).
             */
            Thread.currentThread().setContextClassLoader(dl);
            /* Loads the main class using our custom loader */
            main.invoke(null, new Object[] { new_args });
            
        } catch (InvocationTargetException e){
            logger.fatal(e.toString(), e);
            /* Only throws InvocationTargetException if java.lang.Exception
             * is not a supertype of the exception's cause. That's because
             * we're inheriting from JavaWrapper which has only Exception
             * declared in it's "main" signature.
             */
            Throwable target = e.getCause();
            if(target instanceof Exception)
                throw (Exception)target;
            else
                throw e;
        }

    }
    
    private static void setLogger(){
        
        /* Creates two loggers. */
        
        /* One for local use */
        logger = Logger.getLogger("agent.local");
        
        if(loggerConfig == null)
            BasicConfigurator.configure();
        else
            PropertyConfigurator.configure(loggerConfig);
    }
    
    private static void setInternalLogger(ClassLoader agentLoader)
    	throws Exception
    {
        Class config;
        
        if(loggerConfig == null){
            config = agentLoader.loadClass("org.apache.log4j.BasicConfigurator");
            Method configure = config.getMethod("configure", null);
            configure.invoke(null, null);

        }else{
            config = agentLoader.loadClass("org.apache.log4j.PropertyConfigurator");
            Method configure = config.getMethod("configure", new Class[] { String.class });
            configure.invoke(null, new Object[]{ loggerConfig + ".internal" });
        }
    }
    
    private static void createDefaultHooks(DebugLoader dl)
    	throws Exception
    {
        
        /* Preps and creates the Runnable Hook. */
        RunnableHook rh = new RunnableHook();
        dl.registerHook(rh);
        
        /* Preps and creates the CORBA hook */
        ObjectType [] stubs = getObjectList("stublist");
        ObjectType [] skeletons = getObjectList("skeletonlist");
        if(stubs == null || skeletons == null){
            logger.error("Could not obtain stub/skeleton list. CORBA instrumentation hook will not" +
            		" be enabled.");
            return;
        }
        
        CORBAHook ch = new CORBAHook(stubs, skeletons);
        dl.registerHook(ch);
        
        /* Preps the ORB holder, the callback singleton for CORBAHook-instrumented
         * stubs and skeletons. */
        ORBHolder oh = ORBHolder.getInstance();
        oh.setStampRetrievalStrategy(new JacORBStampRetriever());
    }
    
    private static ObjectType[] getObjectList(String typelistId) 
    	throws IOException, CommException 
    {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        
        try{
            String list = globalAgent.getAttribute(typelistId);
            String [] types = list.split(IConfigurator.LIST_SEPARATOR_CHAR);
            ObjectType [] obtypes = new ObjectType[types.length];
            for(int i = 0; i < types.length; i++)
                obtypes[i] = new ObjectType(types[i]);
            
            return obtypes;
        }catch(Exception e){
            logger.error(e.toString(), e);
            return null;
    	}finally {
            if(in != null) in.close();
        }
    }

    private static void checkOK(byte b)
    	throws CommException{
        if(b != DebuggerConstants.OK){
            throw new CommException("Error while communicating with the global agent." +
            		" Check its log file.");
        }
    }
   
    private static void printUsage(){
        logger.info("Usage: " +
        		    "	LocalAgent {--option=val} [debuggee parameters]\n" +
        		    "	Where required options are:\n" +
        		    "	--main-class*	 Specifies the class containing the main() method in the debuggee.\n" +
        		    "	--central-agent* Specifies the address:port for the central agent\n" +
        		    "	--gid*			 Specifies this node Global Id (GID) \n" +
        		    "	--logger-config	 Specifies a configuration file for the logger\n" +
        		    "	* Starred parameters are required.\n");
    }
}