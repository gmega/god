/*
 * Created on Apr 12, 2005
 * 
 * file: StandardServiceLocator.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;

import ddproto1.exception.IncarnationException;

public class StandardServiceLocator implements IServiceLocator{
    private static StandardServiceLocator instance;
    
    private StandardServiceLocator() { }
    
    public synchronized StandardServiceLocator getInstance(){
        return (instance == null)?instance = new StandardServiceLocator():instance;
    }

    public Object incarnate(Object self, Class childInterface) throws IncarnationException {
        // TODO Auto-generated method stub
        return null;
    }

    public IObjectSpec getChild(Class childInterface) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object incarnate(IObjectSpec spec) throws IncarnationException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<IObjectSpec> rootList() {
        // TODO Auto-generated method stub
        return null;
    }
}
