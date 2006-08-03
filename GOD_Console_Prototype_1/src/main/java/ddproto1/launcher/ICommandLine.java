/*
 * Created on Jul 3, 2006
 * 
 * file: ICommandLine.java
 */
package ddproto1.launcher;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.commons.AttributeAccessException;

public interface ICommandLine extends IConfigurable{
    public String [] renderCommandLine() throws AttributeAccessException;
    
    public void addApplicationParameter(String parameter);
}
