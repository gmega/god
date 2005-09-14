/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.java5;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ddproto1.localagent.Tagger;
import ddproto1.localagent.instrumentation.CORBAHookInitWrapper;
import ddproto1.localagent.instrumentation.IClassLoadingHook;
import ddproto1.localagent.instrumentation.RunnableHook;

public abstract class ClientSideAgent {
    
    private static Logger logger;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        
        setUpLogger();
        
        /** Initializes the Tagger class */
        Byte gid = Byte.parseByte(System.getProperty("agent.local.gid"));
        
        if(gid == null){
            logger.fatal("Global ID for this agent has not been specified. Distributed " +
                    "Debugging is disabled.");
        }
        /** If there's no global ID, there can't be distributed debugging. */
        else{
            Tagger tagger = Tagger.getInstance();
            tagger.setGID(gid);
        
            ClientSideTransformer transformer = new ClientSideTransformer();
            for (IClassLoadingHook modifier : getDefaultModifiers())
                transformer.addModifier(modifier);

            instrumentation.addTransformer(transformer);
        }
    }

    private static List<IClassLoadingHook> getDefaultModifiers() {
        List<IClassLoadingHook> theList = new ArrayList<IClassLoadingHook>();
        theList.add(new CORBAHookInitWrapper());
        theList.add(new RunnableHook());
        return theList;
    }
    
    private static void setUpLogger(){
        String logfile = System.getProperty("log4.configuration.url");
        if(logfile != null){
            try{
                URL url = new URL(logfile);
                PropertyConfigurator.configure(url);
                return;
            }catch(Exception ex){ 
                // On exception rollbacks to default configuration.                
            }
        }
        
        BasicConfigurator.configure();
        logger = Logger.getLogger(ClientSideAgent.class);
    }
}
