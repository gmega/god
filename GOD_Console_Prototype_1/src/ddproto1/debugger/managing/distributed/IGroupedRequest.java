/*
 * Created on Feb 1, 2005
 * 
 * file: IGroupedRequest.java
 */
package ddproto1.debugger.managing.distributed;

import ddproto1.debugger.request.IResolutionListener;

/**
 * @author giuliano
 *
 */
public interface IGroupedRequest extends IResolutionListener{
    public Object queryInterface(Class intf);
}
