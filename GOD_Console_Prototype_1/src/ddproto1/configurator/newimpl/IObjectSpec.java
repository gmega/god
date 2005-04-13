/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpec.java
 */
package ddproto1.configurator.newimpl;

import java.util.List;

import ddproto1.exception.IllegalAttributeException;

public interface IObjectSpec {
    public String getAttribute(String key) throws IllegalAttributeException;
    public String setAttribute(String key) throws IllegalAttributeException;
	public List <IObjectSpec> getChildren();
	public IObjectSpecType getType();
	public IObjectSpec clone();
}
