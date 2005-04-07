/*
 * Created on Jul 28, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ExpectSSHTunnelTest.java
 */

package ddproto1.test;

import java.util.Iterator;

import ddproto1.interfaces.IMessageBox;
import ddproto1.launcher.ExpectSSHTunnel;
import ddproto1.launcher.IShellTunnel;
import ddproto1.util.MessageHandler;
import junit.framework.TestCase;

/**
 * @author giuliano
 *
 */
public class ExpectSSHTunnelTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExpectSSHTunnelTest.class);
    }

    public void testOpen() {
        
        // This is required for all test cases.
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

        IShellTunnel st = new ExpectSSHTunnel();
        
        try{
            st.setAttribute("host", "localhost:22");
            st.setAttribute("user", "dduser");
            st.setAttribute("pass", "dduser123");
            st.setAttribute("pass-type", "plain");
            
            st.open();
            st.close(false);
            
            st.open();
            st.feedCommand("ls -la\n");
            
            Iterator it = st.getStdoutResult().iterator();
            while(it.hasNext()){
                System.out.println((String)it.next());
            }
            
            st.close(false);
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
