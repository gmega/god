/*
 * Created on Apr 12, 2005
 * 
 * file: IObjectSpecType.java
 */
package ddproto1.configurator.newimpl;

import ddproto1.exception.DuplicateSymbolException;

public interface IObjectSpecType {
    public void addChild(IObjectSpecType child) throws DuplicateSymbolException;
    public boolean removeChild(IObjectSpecType child);
    public void addAttribute(String attributeKey);
    public void removeAttribute(String attributeKey);
    public IObjectSpec makeInstance() throws InstantiationException;
    
}
