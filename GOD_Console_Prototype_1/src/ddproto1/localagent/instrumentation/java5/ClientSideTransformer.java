/*
 * Created on Sep 12, 2005
 * 
 * file: ClientSideAgent.java
 */
package ddproto1.localagent.instrumentation.java5;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;

public class ClientSideTransformer implements ClassFileTransformer{

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return classfileBuffer;
    }

}
