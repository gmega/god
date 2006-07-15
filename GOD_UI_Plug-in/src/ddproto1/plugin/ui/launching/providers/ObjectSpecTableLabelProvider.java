/*
 * Created on Sep 3, 2005
 * 
 * file: ObjectSpecPropertyProvider.java
 */
package ddproto1.plugin.ui.launching.providers;

import java.io.IOException;
import java.net.URL;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;


import ddproto1.configurator.IAttribute;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.plugin.ui.DDUIPlugin;

public class ObjectSpecTableLabelProvider extends LabelProvider implements ITableLabelProvider {

    private ObjectSpecTableContentProvider contentProvider;
    private Image ctxImage;
    
    public ObjectSpecTableLabelProvider(ObjectSpecTableContentProvider contentProvider){
        try{
            Bundle bundle = DDUIPlugin.getDefault().getBundle();
            URL ctxImageURL = bundle.getEntry("images/ctx.png");
            if(ctxImageURL != null)
                ctxImage = new Image(Display.getCurrent(), new ImageData(ctxImageURL.openStream()));
            this.contentProvider = contentProvider;
        }catch(IOException ex){
            // TODO Issue a warning or something.
        }
    }
    
    public Image getColumnImage(Object element, int columnIndex) {
        IAttribute attribute = (IAttribute)element;
        try{
            if(contentProvider.currentSpec().isContextAttribute(attribute.attributeKey()) && columnIndex == 0)
                return ctxImage;
            else
                return null;
        }catch(AttributeAccessException ex){
            return null; // TODO Return something that shows an error occurred. 
        }
    }

    public String getColumnText(Object element, int columnIndex) {
        IAttribute attribute = (IAttribute)element;
        if(columnIndex == 0) return attribute.attributeKey();
        try{
            return contentProvider.currentSpec().getAttribute(attribute.attributeKey());
        }catch(IllegalAttributeException ex){
            ex.printStackTrace();
            return "error!";
        }catch(UninitializedAttributeException ex){
            return "uninitialized";
        }
    }
}
