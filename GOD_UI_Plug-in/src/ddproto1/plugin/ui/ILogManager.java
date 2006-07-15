/*
 * Created on Oct 22, 2005
 * 
 * file: ILogManager.java
 */
package ddproto1.plugin.ui;

import org.apache.log4j.Logger;

public interface ILogManager {
    public Logger getLogger(String logger);
    public Logger getLogger(Class  target);
}
