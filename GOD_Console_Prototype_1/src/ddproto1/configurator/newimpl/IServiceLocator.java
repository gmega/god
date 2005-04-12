/*
 * Created on Apr 12, 2005
 * 
 * file: IServiceLocator.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;

import ddproto1.exception.IncarnationException;

public interface IServiceLocator {
    public Object incarnate(Object self, Class childInterface) throws IncarnationException;
    public IObjectSpec getChild(Class childInterface);
    public Object incarnate(IObjectSpec spec) throws IncarnationException;
    public List <IObjectSpec> rootList();
}
