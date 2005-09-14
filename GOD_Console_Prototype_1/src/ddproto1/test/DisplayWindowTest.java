/*
 * Created on Sep 12, 2005
 * 
 * file: DisplayWindowTest.java
 */
package ddproto1.test;

import ddproto1.interfaces.ISemaphore;
import ddproto1.primitiveGUI.DisplayWindow;
import ddproto1.primitiveGUI.IOpenListener;
import ddproto1.util.Semaphore;
import junit.framework.TestCase;

public class DisplayWindowTest extends TestCase implements IOpenListener{

    private DisplayWindow disp;
    private ISemaphore sema = new Semaphore(0);
    
    protected void setUp() throws Exception {
        super.setUp();
        disp = new DisplayWindow("Debug");
        disp.addOpenListener(this);
        disp.openAsync();
    }

    /*
     * Test method for 'primitiveGUI.DisplayWindow.println(String)'
     */
    public void testPrintln() {
        try {
            sema.p();
            disp.println("Olá, eu sou o Giuliano");
            disp.println("Estou imprimindo lixo!!");
            for (int i = 0; i < 15; i++) {
                disp.println(Integer.toString(i));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }
    
    public static void main(String [] args)
        throws Exception
    {
        DisplayWindowTest dwt = new DisplayWindowTest();
        dwt.setUp();
        dwt.testPrintln();
    }

    public synchronized void notifyOpening() {
        sema.v();
    }

}
