/*
 * Created on Sep 8, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: LinePumper.java
 */

package ddproto1.configurator;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.server.IRequestHandler;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.util.ByteMessage;

/**
 * @author giuliano
 *
 */
public class InfoCarrierWrapper implements IRequestHandler {
   
    private IInfoCarrier [] info;

    public InfoCarrierWrapper(IInfoCarrier [] info){
        this.info = info;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.configurator.IConfigRequestHandler#handleRequest(java.lang.Integer, java.io.BufferedReader, java.io.BufferedWriter)
     */
    public ByteMessage handleRequest(Byte gid, ByteMessage req) {
        byte vgid = gid.byteValue();
        
        ByteMessage ans;
        
        /* If gid is out of range, returns an error. */
        if(vgid < 0 || vgid >= info.length){
            ans = new ByteMessage(1);
            ans.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            ans.writeAt(0, DebuggerConstants.ICW_INVALID_GID);
            return ans;
        }
        
        /* Tries to obtain the correct property */
        IInfoCarrier ninfo = info[vgid];
        try	{
            String key = new String(req.getMessage());
            String property = ninfo.getAttribute(key);
            ans = new ByteMessage(property.getBytes());
            ans.setStatus(DebuggerConstants.OK);
            return ans;
            
        }catch(IllegalAttributeException e){
            ans = new ByteMessage(1);
            ans.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            ans.writeAt(0, DebuggerConstants.ICW_ILLEGAL_ATTRIBUTE);
            return ans;
        }
    }
}
