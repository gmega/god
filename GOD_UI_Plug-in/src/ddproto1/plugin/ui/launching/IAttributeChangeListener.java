/*
 * Created on Sep 9, 2005
 * 
 * file: IAttributeChangeListener.java
 */
package ddproto1.plugin.ui.launching;

import ddproto1.configurator.IAttribute;

public interface IAttributeChangeListener {
    public void attributeChanged(IAttribute attribute, String value);
}
