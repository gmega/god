/*
 * Created on 5/09/2006
 * 
 * file: GenericSourceLookupDirector.java
 */
package ddproto1.sourcemapper;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

public class GenericSourceLookupDirector extends AbstractSourceLookupDirector{

    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
        
    }

}
