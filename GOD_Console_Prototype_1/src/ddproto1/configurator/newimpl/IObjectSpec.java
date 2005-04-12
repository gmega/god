/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;

public interface IObjectSpec {
    public String getAttribute(String key);
    public String setAttribute(String key);
    public List <IObjectSpec> getChildren();
}
