/*
 * Created on Sep 4, 2005
 * 
 * file: MissingChildrenLabelProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import org.eclipse.jface.viewers.LabelProvider;


import ddproto1.configurator.IIntegerInterval;

public class MissingChildrenLabelProvider extends LabelProvider {

    private MissingChildrenContentProvider provider;
    
    public MissingChildrenLabelProvider(MissingChildrenContentProvider provider){
        this.provider = provider;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
        String key = (String) element;
        IIntegerInterval childrenCount = provider.getFromCache(key);
        assert childrenCount != null;
        return key + " (" + childrenCount.getMin() + ", "
                + ((childrenCount.getMax() >= Integer.MAX_VALUE / 2) ? "infinite"
                        : Integer.toString(childrenCount.getMax())) 
                        + ")";
    }

}
