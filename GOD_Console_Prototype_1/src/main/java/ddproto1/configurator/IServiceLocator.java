/*
 * Created on Apr 12, 2005
 * 
 * file: IServiceLocator.java
 */
package ddproto1.configurator;

import java.util.NoSuchElementException;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.IncarnationException;

public interface IServiceLocator {

    public IConfigurable incarnate(IObjectSpec ospec, boolean createNew) throws IncarnationException;

    /**
     * Same as incarnate(ospec, false).
     * 
     * @param ospec
     * @return
     * @throws IncarnationException
     */
    public IConfigurable incarnate(IObjectSpec ospec) throws IncarnationException;
    
    
    public IObjectSpec getMetaobject(Object incarnation) throws NoSuchElementException;
}
