/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.java5;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ddproto1.localagent.PIManagementDelegate;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.CORBA.ORBHolder;
import ddproto1.localagent.CORBA.orbspecific.JacORBStampRetriever;
import ddproto1.localagent.instrumentation.bcel.BCELClientSideTransformer;
import ddproto1.localagent.instrumentation.bcel.BCELRunnableHook;
import ddproto1.localagent.instrumentation.bcel.CORBAHookInitWrapper;
import ddproto1.localagent.instrumentation.bcel.IClassLoadingHook;
import ddproto1.localagent.instrumentation.bcel.MainThreadTainter;

public abstract class ClientSideAgent {
    
    private static Logger logger;
    
    private static final String [] filteredPackages = new String [] { };

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        
        setUpLogger();
        
        /** Initializes the Tagger class */
        Byte gid = Byte.parseByte(System.getProperty("agent.local.gid"));
        
        try {
            if (gid == null) {
                logger
                        .fatal("Global ID for this agent has not been specified. Distributed "
                                + "Debugging is disabled.");
            }
            /** If there's no global ID, there can't be distributed debugging. */
            else {
                /*
                 * Preps the Tagger and the ORB holder, the callback singletons 
                 * for CORBAHook-instrumented stubs and skeletons, respectively.
                 * 
                 * NOTE: This might be an issue. Loading the ORBHolder 
                 * and the JacORBStampRetriever may push some classes 
                 * a few loaders up. Since I don't quite understand how
                 * the transformation agent loads the classes it uses, 
                 * we might get into trouble. If that's the case, I could
                 * use the application class loader to manually load these
                 * classes and do the inserting through reflection.
                 */
                PIManagementDelegate delegate = new PIManagementDelegate(new JacORBStampRetriever());
                Tagger tagger = Tagger.getInstance();
                tagger.setGID(gid);
                tagger.setPICManagementDelegate(delegate);
                ORBHolder oh = ORBHolder.getInstance();
                oh.setPICManagementDelegate(delegate);
                

                BCELClientSideTransformer transformer = new BCELClientSideTransformer();
                for (IClassLoadingHook modifier : getDefaultModifiers(transformer))
                    transformer.addModifier(modifier);

                instrumentation.addTransformer(transformer);
            }
        } catch (Exception ex) {
            logger.error("Failed to initialize instrumentation hooks. Distributed Debugging " +
                    "is disabled.", ex);
        }
    }

    private static List<IClassLoadingHook> getDefaultModifiers(BCELClientSideTransformer transformer) {
        List<IClassLoadingHook> theList = new ArrayList<IClassLoadingHook>();
        theList.add(new MainThreadTainter(transformer));
        theList.add(new CORBAHookInitWrapper());
        theList.add(new BCELRunnableHook(getFilteredPackages(), false));
        return theList;
    }
    
    private static void setUpLogger(){
        String logfile = System.getProperty("log4.configuration.url");
        if(logfile != null){
            try{
                URL url = new URL(logfile);
                PropertyConfigurator.configure(url);
                System.err.println("Log4j configured from source " + url);
                return;
            }catch(Exception ex){ 
                System.err.println("Failed to configure logger. Falling back to default configuration.");      
            }
        }
        
        BasicConfigurator.configure();
        logger = Logger.getLogger(ClientSideAgent.class);
    }
    
    private static Set<String> getFilteredPackages(){
        Set<String> filteredSet = new HashSet<String>();
        for(String prefix : filteredPackages)
            filteredSet.add(prefix);
        return filteredSet;
    }
}
