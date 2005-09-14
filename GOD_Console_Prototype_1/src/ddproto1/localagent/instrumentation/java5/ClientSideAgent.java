/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.java5;

import java.lang.instrument.Instrumentation;

import org.apache.log4j.BasicConfigurator;

import ddproto1.localagent.instrumentation.CORBAHookInitWrapper;
import ddproto1.localagent.instrumentation.IClassLoadingHook;
import ddproto1.localagent.instrumentation.RunnableHook;

public abstract class ClientSideAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        BasicConfigurator.configure();
        ClientSideTransformer transformer = new ClientSideTransformer();
        for (IClassLoadingHook modifier : getDefaultModifiers())
            transformer.addModifier(modifier);

        instrumentation.addTransformer(transformer);
    }

    private static IClassLoadingHook [] getDefaultModifiers() {
        return new IClassLoadingHook[] {
                new CORBAHookInitWrapper(),
                new RunnableHook() };
    }
}
