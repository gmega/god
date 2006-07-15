/*
 * Created on Sep 3, 2005
 * 
 * file: ObjectSpecLabelAdapter.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.io.IOException;
import java.net.URL;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import ddproto1.configurator.IObjectSpec;
import ddproto1.plugin.ui.DDUIPlugin;

public class ObjectSpecLabelAdapter extends LabelProvider {

    private Image spec;
    
    public ObjectSpecLabelAdapter()
        throws IOException
    {
        Bundle bundle = DDUIPlugin.getDefault().getBundle();
        URL specImageURL = bundle.getEntry("images/spec.png");
        if(specImageURL != null)
            spec = new Image(Display.getCurrent(), new ImageData(specImageURL.openStream()));
    }
    
    public Image getImage(Object element) {
        if(element instanceof IObjectSpec)
            return spec;
        else return null;
    }

    public String getText(Object element) {
        if(element instanceof String) return (String)element;
        else if(element instanceof IObjectSpec) return ((IObjectSpec)element).getType().toString();
        else return "Unknown";
    }

    public void dispose() {
        super.dispose();
        if(spec != null) spec.dispose();
    }
}
