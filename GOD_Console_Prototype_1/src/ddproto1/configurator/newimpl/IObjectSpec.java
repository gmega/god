/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;
import ddproto1.exception.UninitializedAttributeException;

public interface IObjectSpec {
    public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException;
    public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException;
    public void addChild(IObjectSpec child) throws IllegalAttributeException;
    
    public List<IObjectSpec> getChildren();
	public IObjectSpecType getType();
}
