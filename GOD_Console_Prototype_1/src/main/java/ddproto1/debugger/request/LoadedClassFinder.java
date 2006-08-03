/*
 * Created on Nov 17, 2005
 * 
 * file: LoadedClassFinder.java
 */
package ddproto1.debugger.request;

import java.util.List;

import com.sun.jdi.VirtualMachine;

import ddproto1.debugger.request.IDeferrableRequest.IPrecondition;
import ddproto1.debugger.request.IDeferrableRequest.IResolutionContext;

public class LoadedClassFinder implements IEagerPreconditionChecker {
    
    public LoadedClassFinder(VirtualMachine theVM){
        
    }
    
    public List<IResolutionContext> preconditionMatches(
            IPrecondition precondition) {
        return null;
    }

}
