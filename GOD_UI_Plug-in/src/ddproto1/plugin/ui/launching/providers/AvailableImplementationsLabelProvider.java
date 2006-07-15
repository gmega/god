/*
 * Created on Sep 6, 2005
 * 
 * file: AvailableImplementationsLabelProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import org.eclipse.jface.viewers.LabelProvider;

import ddproto1.configurator.IObjectSpecType;
import ddproto1.exception.commons.IllegalAttributeException;

public class AvailableImplementationsLabelProvider extends LabelProvider{

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
        IObjectSpecType specType = (IObjectSpecType)element;
        try{
            return specType.getConcreteType();
        }catch(IllegalAttributeException ex){
            return "Error";
        }
    }

}
