/*
 * Created on Sep 5, 2005
 * 
 * file: IImplementationScanner.java
 */
package ddproto1.configurator.plugin;

import java.io.IOException;

import org.xml.sax.SAXException;

import ddproto1.configurator.IObjectSpecType;
import ddproto1.configurator.IllegalProtocolException;
import ddproto1.configurator.SpecNotFoundException;


public interface IImplementationScanner {
    public Iterable<IObjectSpecType> retrieveImplementationsOf(String interfaceSet) 
    		throws IOException, SAXException, SpecNotFoundException, IllegalProtocolException;
    
    public void asyncRetrieveImplementationsOf(String interfaceSet);
    public void addAnswerListener(IImplementationScannerListener listener);
    public void removeAnswerListener(IImplementationScannerListener listener);
}
