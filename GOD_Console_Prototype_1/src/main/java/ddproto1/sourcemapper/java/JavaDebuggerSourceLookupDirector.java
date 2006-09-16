/*
 * Created on 8/09/2006
 * 
 * file: JavaDebuggerSourceLookupDirector.java
 */
package ddproto1.sourcemapper.java;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

public class JavaDebuggerSourceLookupDirector extends AbstractSourceLookupDirector{

    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
    }

}
