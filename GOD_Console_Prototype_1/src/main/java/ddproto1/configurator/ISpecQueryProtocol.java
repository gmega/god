/*
 * Created on Apr 28, 2005
 * 
 * file: ISpecQueryProtocol.java
 */
package ddproto1.configurator;

import java.util.Map;
import java.util.Set;

public interface ISpecQueryProtocol extends IObjectSpecType{
    public boolean containsAttribute(String key, Map <String, String> state);
    public IIntegerInterval computeResultingChildrenConstraints(
            String type, Map<String, String> state);
    public IIntegerInterval computeResultingChildrenConstraints(
            IObjectSpecType type, Map<String, String> state);
    public Set <String> allSupportedChildrenTypes(Map<String,String>state);
    public Set <String> getRestrictedKeys(Map<String,String>state);
}   
