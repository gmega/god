/*
 * Created on Nov 16, 2005
 * 
 * file: IEagerPreconditionChecker.java
 */
package ddproto1.debugger.request;

import java.util.List;

import ddproto1.debugger.request.IDeferrableRequest.IPrecondition;
import ddproto1.debugger.request.IDeferrableRequest.IResolutionContext;

public interface IEagerPreconditionChecker {
    public List<IResolutionContext> preconditionMatches(IPrecondition precondition);
}
