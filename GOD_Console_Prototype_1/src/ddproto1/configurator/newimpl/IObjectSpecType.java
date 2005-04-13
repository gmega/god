/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpecType.java
 */
package ddproto1.configurator.newimpl;

import java.util.Set;

import ddproto1.exception.DuplicateSymbolException;

public interface IObjectSpecType {
	public static final String CONCRETE_TYPE_ATTRIBUTE = "concrete-type";
	
	public void addChild(IObjectSpecType child) throws DuplicateSymbolException;
    public boolean removeChild(IObjectSpecType child);
	
    public String getConcreteType();
	public void addAttribute(String attributeKey);
    public void removeAttribute(String attributeKey);
	
	public void lockForReading();
	public void lockForWriting();
	public void unlock();
	
	public Set <String> attributeSet();
	public boolean containsAttribute(String key);
    public IObjectSpec makeInstance() throws InstantiationException;
}
