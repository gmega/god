/*
 * Created on Sep 8, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: LinePumper.java
 */

package ddproto1.configurator;

import java.util.NoSuchElementException;

import ddproto1.util.commons.ByteMessage;
import ddproto1.commons.DebuggerConstants;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.debugger.managing.INodeManagerRegistry;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.server.IRequestHandler;
import ddproto1.exception.commons.AttributeAccessException;

/**
 * @author giuliano
 *
 */
public class PropertyHandler implements IRequestHandler {
   
    private INodeManagerRegistry vmmf = VMManagerFactory.getInstance();

    public PropertyHandler(){ }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.configurator.IConfigRequestHandler#handleRequest(java.lang.Integer, java.io.BufferedReader, java.io.BufferedWriter)
     */
    public ByteMessage handleRequest(Byte gid, ByteMessage req) {

        ByteMessage ans;
        IConfigurable configurable;
        
        /** Attempts to acquire a reference to the IConfigurable
         * indexed by this ID.
         */
        try{
            configurable = vmmf.getNodeManager(gid);
        }catch(NoSuchElementException ex){
            /** Invalid GID. */
            ans = new ByteMessage(1);
            ans.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            ans.writeAt(0, DebuggerConstants.ICW_INVALID_GID);
            return ans;
        }
        
        /** Now attempts to obtain the correct property */
        try	{
            String key = new String(req.getMessage());
            String property = configurable.getAttribute(key);
            ans = new ByteMessage(property.getBytes());
            ans.setStatus(DebuggerConstants.OK);
            return ans;
            
        }catch(AttributeAccessException e){
            ans = new ByteMessage(1);
            ans.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
            ans.writeAt(0, DebuggerConstants.ICW_ILLEGAL_ATTRIBUTE);
            return ans;
        }
    }
}
