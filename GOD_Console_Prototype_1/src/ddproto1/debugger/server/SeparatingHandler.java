/*
 * Created on Oct 15, 2004
 * 
 * file: SeparatingHandler.java
 */
package ddproto1.debugger.server;

import java.util.HashMap;
import java.util.Map;

import ddproto1.util.commons.ByteMessage;
import ddproto1.commons.DebuggerConstants;

/**
 * Simple class that classifies request handlers according to the
 * switch_byte field value of incoming messages. Can be chained
 * for complex processing.
 * 
 * @author giuliano
 *
 */
public class SeparatingHandler implements IRequestHandler{

    private byte idx;
    private Map handlers = new HashMap();
    
    
    public SeparatingHandler(byte idx){
        this.idx = idx;
    }
    
    public ByteMessage handleRequest(Byte gid, ByteMessage request) {
        
        ByteMessage oup;
        
        /* We do it like this so we can use the status field as 
         * separating field as well.
         */
        byte [] req = request.getBytes();
        
        /* Must contain the type byte */
        if(req.length <= idx + 1){
            oup = new ByteMessage(0);
            oup.setStatus(DebuggerConstants.PROTOCOL_ERR);
            return oup;
        }
        
        Byte type = new Byte(req[idx]);

        if(!handlers.containsKey(type)){
            oup = new ByteMessage(1);
            oup.setStatus(DebuggerConstants.PROTOCOL_ERR);
            oup.writeAt(0, DebuggerConstants.NO_HANDLER_ERR);
            return oup;
        }
        
        IRequestHandler irh = (IRequestHandler)handlers.get(type);
        
        return irh.handleRequest(gid, request);
    }
    
    public void registerHandler(byte type, IRequestHandler rh){
        handlers.put(new Byte(type), rh);
    }
    
    public IRequestHandler unregisterHandler(byte type, IRequestHandler rh){
        return (IRequestHandler)handlers.remove(new Byte(type));
    }

}
