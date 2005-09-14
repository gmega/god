/*
 * Created on Sep 21, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: GlobalAgentProxyImpl.java
 */

package ddproto1.localagent.client;

import org.apache.log4j.Logger;

import ddproto1.util.commons.ByteMessage;
import ddproto1.util.commons.Event;
import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.commons.CommException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UnsupportedException;

/**
 * @author giuliano
 *
 */
class GlobalAgentProxyImpl implements IGlobalAgent{
    
    private static final int RESPONSE_TIMEOUT = 0;
    
    private byte localgid;

    private IConnectionManager icmgr;
    private Logger logger = Logger.getLogger("agent.local");
    
    protected GlobalAgentProxyImpl(IConnectionManager icmgr, byte localgid){
        this.icmgr = icmgr;
        /* TODO See if this is really necessary
         * Hooks protocol-specific code to the generic connection
         * management mechanism */
        icmgr.setProtocolHook(new DDProto1_ProtocolHook());
        this.localgid = localgid;
    }

    /* (non-Javadoc)
     * @see ddproto1.localagent.client.IGlobalAgent#getAttribute(java.lang.String)
     */
    public String getAttribute(String key) 
    	throws CommException
    {
        /* Acquires a connection */
        IConnection conn = icmgr.acquire();
        try{
            /* Transforms the string into bytes and then into a ByteMessage */
            byte [] _key = key.getBytes();
            ByteMessage bm = new ByteMessage(_key);
            bm.setStatus(DebuggerConstants.REQUEST);
        
            /* Pump it through the connection and waits for an answer 
             * TODO allow timeout for incoming answers */
            conn.send(bm);
            bm = conn.recv(RESPONSE_TIMEOUT);
            checkOK(bm);
        
            /* Repacks the byte stream and returns it */
            String answer = new String(bm.getMessage());
        
            return answer;
            
        }catch(CommException e){
            throw e;
        }catch(Exception e){
            logger.error(e.toString(), e);
            return null;
        }finally{
            if(conn != null) conn.release();
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.localagent.client.IGlobalAgent#syncNotify(ddproto1.localagent.client.Event)
     */
    public int syncNotify(Event e) 
    	throws CommException
    {
        /* Acquires a connection */
        IConnection conn = icmgr.acquire();
        try {
            /* Transforms the event into bytes and then into a ByteMessage */
            byte[] _event = e.toByteStream();
            ByteMessage bm = new ByteMessage(_event);
            bm.setStatus(DebuggerConstants.NOTIFICATION);

            /*
             * Pump it through the connection and waits for an answer TODO allow
             * timeout for incoming answers
             */
            conn.send(bm);
            bm = conn.recv(RESPONSE_TIMEOUT);
            checkOK(bm);
            
            return (bm.getSize() >= 1)?bm.get(DebuggerConstants.EVENT_TYPE_IDX):bm.getStatus();
            
        }catch(CommException ex){
            throw ex;
        }catch(Exception ex){
            logger.error(ex.toString(), ex);
            throw new NestedRuntimeException(ex);
        } finally {
            if(conn != null) conn.release();
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.localagent.client.IGlobalAgent#asyncNotify(ddproto1.localagent.client.Event)
     */
    public void asyncNotify(Event e) 
    	throws CommException
    {
        throw new UnsupportedException("This operation has no defined semantics yet.");
        
    }
    
//    private IConnection startRequest()
//    	throws CommException
//    {
//        IConnection conn = null;
//        
//        try{
//            conn = icmgr.acquire();
//            conn.send(greet);
//            ByteMessage ans = conn.recv(RESPONSE_TIMEOUT);
//            checkOK(ans);
//            return conn;
//        }catch(CommException e){
//            logger.error("Failed to acquire connection.");
//            if(conn != null) conn.release();
//            throw e;
//        }
//    }
//    
//    private void endRequest(IConnection conn)
//    	throws CommException
//    {
//        try{
//            conn = icmgr.acquire();
//            conn.send(bye);
//            ByteMessage ans = conn.recv(RESPONSE_TIMEOUT);
//            checkOK(ans);
//        }catch(CommException e){
//            throw e;
//        }finally{
//            conn.release();
//        }
//    }

    /* (non-Javadoc)
     * @see ddproto1.localagent.client.IGlobalAgent#dispose()
     */
    public void dispose() 
    	throws CommException
    {
        icmgr.closeAll();        
    }
    
    private void checkOK(ByteMessage bm) throws CommException{
        if(!(bm.getStatus() == DebuggerConstants.OK))
            throw new CommException("Failed to establish a connection - reason: protocol failure.\n" +
            		" Expected " + DebuggerConstants.OK + " but got " + bm.getStatus() + " instead.");
    }
    
    private class DDProto1_ProtocolHook implements IProtocolHook{

        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IProtocolHook#pre_open(ddproto1.localagent.client.IConnection)
         */
        public void pre_open(IConnection conn) throws CommException {
            ByteMessage opening = new ByteMessage(1);
            opening.setStatus(DebuggerConstants.START_REQUEST);
            opening.writeAt(0, localgid);
            conn.send(opening);
            ByteMessage receive = conn.recv(0);
            checkOK(receive);
        }

        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IProtocolHook#pre_close(ddproto1.localagent.client.IConnection)
         */
        public void pre_close(IConnection conn) throws CommException {
            ByteMessage closure = new ByteMessage(0);
            closure.setStatus(DebuggerConstants.END_REQUEST);
            conn.send(closure);
            ByteMessage receive = conn.recv(0);
            checkOK(receive);
        }
        
    }
}
