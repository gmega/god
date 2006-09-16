/*
 * Created on 5/09/2006
 * 
 * file: JavaSourceLookupParticipant.java
 */
package ddproto1.sourcemapper.java;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.VMDisconnectedException;

import ddproto1.debugger.managing.JavaStackframe;

public class JavaSourceLookupParticipant extends AbstractSourceLookupParticipant{

    public String getSourceName(Object object) throws CoreException {
        if(object instanceof JavaStackframe){
            JavaStackframe jsf = (JavaStackframe)object;
            try{
                Location loc = jsf.getLocation();
                if(loc == null) return "<invalid>";
                return loc.sourcePath();
            }catch(AbsentInformationException ex){
                return null;
            }catch(ObjectCollectedException ex){
                return null;
            }catch(VMDisconnectedException ex){
                return null;
            }
        }
        
        return null;
    }

}
