/*
 * Created on Nov 6, 2005
 * 
 * file: ILogManager.java
 */
package ddproto1.util;

import org.apache.log4j.Logger;

/**
 * Delegate for log managing singletons.
 * 
 * @author giuliano
 */
public interface ILogManager {
    public Logger getLogger(Class  c);
    public Logger getLogger(String name);
}
