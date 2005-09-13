/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.java5;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.URL;

public class ClientSideAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation){


        instrumentation.addTransformer(new ClientSideTransformer());
    }
}
