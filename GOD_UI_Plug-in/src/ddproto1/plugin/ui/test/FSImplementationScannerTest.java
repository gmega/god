/*
 * Created on Sep 5, 2005
 * 
 * file: FSImplementationScannerTest.java
 */
package ddproto1.plugin.ui.test;

import ddproto1.configurator.FileLister;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.SpecLoader;
import ddproto1.plugin.ui.launching.FSImplementationScanner;
import ddproto1.plugin.ui.launching.IImplementationScanner;
import ddproto1.plugin.ui.launching.IImplementationScannerListener;
import ddproto1.test.BasicSpecTest;


public class FSImplementationScannerTest extends BasicSpecTest implements IImplementationScannerListener {

    private Iterable<IObjectSpecType> sData = null;
    
    public synchronized void testFSLookup(){
        try {
            FSImplementationScanner scanner = new FSImplementationScanner(true,
                    new SpecLoader(null, TOCurl));
            scanner.registerURLLister(new FileLister());
            scanner.addAnswerListener(this);
            scanner.asyncRetrieveImplementationsOf("shell-tunnel");
            while (sData == null) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            for (IObjectSpecType data : sData) {
                System.out.println(data);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    public synchronized void receiveAnswer(Iterable<IObjectSpecType> answerList) {
        sData = answerList;
        this.notify();
    }

}
