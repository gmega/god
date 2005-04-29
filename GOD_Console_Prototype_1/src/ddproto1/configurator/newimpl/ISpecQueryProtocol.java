/*
 * Created on Apr 28, 2005
 * 
 * file: ISpecQueryProtocol.java
 */
package ddproto1.configurator.newimpl;

import java.util.Map;

public interface ISpecQueryProtocol extends IObjectSpecType{
    public boolean containsAttribute(String key, Map <String, String> state);
    public int childrenNumber(IObjectSpecType type, Map<String,String> state);
}   
