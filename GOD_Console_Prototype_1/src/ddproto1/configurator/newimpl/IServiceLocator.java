/*
 * Created on Apr 12, 2005
 * 
 * file: IServiceLocator.java
 */
package ddproto1.configurator.newimpl;

import java.util.NoSuchElementException;

import ddproto1.configurator.IConfigurable;
import ddproto1.exception.IncarnationException;

public interface IServiceLocator {
    public IConfigurable incarnate(IObjectSpec ospec) throws IncarnationException;
	public IObjectSpec getMetaobject(Object incarnation) throws NoSuchElementException;
}
