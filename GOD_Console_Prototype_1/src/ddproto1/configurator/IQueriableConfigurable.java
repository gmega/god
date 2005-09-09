/*
 * Created on Aug 8, 2005
 * 
 * file: IInfoCarrier.java
 */
package ddproto1.configurator;

import java.util.Set;

public interface IQueriableConfigurable extends IConfigurable{
    /**
     * Returns the current attribute key set. Might change with time.  
     * 
     * @return
     */
    public Set <String> getAttributeKeys();

}
