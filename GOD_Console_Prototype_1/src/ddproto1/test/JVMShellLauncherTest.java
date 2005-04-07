/*
 * Created on Jul 30, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: JVMShellLauncherTest.java
 */

package ddproto1.test;

import ddproto1.interfaces.IMessageBox;
import ddproto1.launcher.ExpectSSHTunnel;
import ddproto1.launcher.JVMShellLauncher;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class JVMShellLauncherTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JVMShellLauncherTest.class);
    }

    public void testLaunch() {
        
        // We begin by setting the usual stuff
        IMessageBox stdout = new IMessageBox(){
            public void println(String s){
                System.out.println(s);
            }
            
            public void print(String s){
                System.out.print(s);
            }
        };
        
        IMessageBox stderr = new IMessageBox(){
            public void println(String s){
                System.err.println(s);
            }
            
            public void print(String s){
                System.err.print(s);
            }
        };
        
        MessageHandler mh = MessageHandler.getInstance();
        mh.setErrorOutput(stderr);
        mh.setStandardOutput(stdout);

        String tprefix = ExpectSSHTunnel.class.getName();
        JVMShellLauncher sl = new JVMShellLauncher();
        
        try{
            sl.setAttribute("main-class", "SimpleClass");
            sl.setAttribute("tunnel-class", "ddproto1.launcher.ExpectSSHTunnel");
            sl.setAttribute("classpath", "/home/giuliano/private/trabalhos/workspace/simpleapp");
            sl.setAttribute("jdwp-port", "2000");
            sl.setAttribute(tprefix + ".host", "localhost:22");
            sl.setAttribute(tprefix + ".user", "dduser");
            sl.setAttribute(tprefix + ".pass", "dduser123");
            sl.setAttribute(tprefix + ".pass-type", "plain");
            sl.launch();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
