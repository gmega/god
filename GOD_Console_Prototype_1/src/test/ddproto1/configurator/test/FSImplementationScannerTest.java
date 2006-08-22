/*
 * Created on Sep 5, 2005
 * 
 * file: FSImplementationScannerTest.java
 */
package ddproto1.configurator.test;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.BundleEntryLister;
import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.plugin.FSImplementationScanner;
import ddproto1.configurator.plugin.IImplementationScannerListener;
import ddproto1.util.TestUtils;

public class FSImplementationScannerTest extends BasicSpecTest implements IImplementationScannerListener {

    private Iterable<IObjectSpecType> sData = null;
    
    public synchronized void testFSLookup(){
        try {
            TestUtils.setPluginTest(true);
            FSImplementationScanner scanner = new FSImplementationScanner(true, this.getDefaultSpecLoader());
            scanner.registerURLLister(new BundleEntryLister(GODBasePlugin.getDefault().getBundle()));
            scanner.addAnswerListener(this);
            scanner.asyncRetrieveImplementationsOf("shell-tunnel");
            while (sData == null) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            
            int length = 0;
            
            for (IObjectSpecType data : sData) {
                assertTrue(data.getInterfaceType().equals("shell-tunnel"));
                length++;
            }
            
            assertTrue(length == 1);
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
