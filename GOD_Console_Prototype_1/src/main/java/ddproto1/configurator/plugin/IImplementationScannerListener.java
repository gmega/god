/*
 * Created on Sep 5, 2005
 * 
 * file: IImplementationScannerListener.java
 */
package ddproto1.configurator.plugin;

import ddproto1.configurator.IObjectSpecType;


public interface IImplementationScannerListener {
    public void receiveAnswer(Iterable<IObjectSpecType> answerList);
}
