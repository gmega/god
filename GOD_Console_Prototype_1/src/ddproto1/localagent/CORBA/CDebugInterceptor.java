/*
 * Created on Sep 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ClientInterceptor.java
 */

package ddproto1.localagent.CORBA;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;

import ddproto1.commons.DebuggerConstants;
import ddproto1.commons.Event;
import ddproto1.exception.CommException;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;
import ddproto1.util.traits.ConversionTrait;

/**
 * @author giuliano
 *
 */
public class CDebugInterceptor extends LocalObject implements ClientRequestInterceptor{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static Tagger tagger = Tagger.getInstance();
    
    private String name;
    private int upcall_context, downcall_context;
    private int dtslot, ltslot, tpslot;
    private IGlobalAgent global;
    private ConversionTrait fh = ConversionTrait.getInstance();
    
    private static final Logger logger = Logger.getLogger(CDebugInterceptor.class);
    
    public CDebugInterceptor(String name, int upctxId, int downctxId, int dtslot, int ltslot, int tpslot)
    	throws UnknownHostException, IOException
    {
        global = GlobalAgentFactory.getInstance().resolveReference();
        this.name = name;
        this.upcall_context= upctxId;
        this.downcall_context = downctxId;
        this.dtslot = dtslot;
        this.ltslot = ltslot;
        this.tpslot = tpslot;
    }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ClientRequestInterceptorOperations#receive_exception(org.omg.PortableInterceptor.ClientRequestInfo)
     */
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest { this.setForFourth(ri); }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ClientRequestInterceptorOperations#receive_other(org.omg.PortableInterceptor.ClientRequestInfo)
     */
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest { this.setForFourth(ri); }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ClientRequestInterceptorOperations#receive_reply(org.omg.PortableInterceptor.ClientRequestInfo)
     */
    public void receive_reply(ClientRequestInfo ri) { this.setForFourth(ri); }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ClientRequestInterceptorOperations#send_poll(org.omg.PortableInterceptor.ClientRequestInfo)
     */
    public void send_poll(ClientRequestInfo ri) { }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ClientRequestInterceptorOperations#send_request(org.omg.PortableInterceptor.ClientRequestInfo)
     */
    public void send_request(ClientRequestInfo ri) throws ForwardRequest {
        logger.debug("Entering send_request");
                        
        /* Obtains the gid for the current distributed thread */
        int dtgid, ltgid, tpsiz;
        try{
            dtgid = ri.get_slot(dtslot).extract_long();
            ltgid = ri.get_slot(ltslot).extract_long();
            tpsiz = ri.get_slot(tpslot).extract_long();
            logger.debug(name + " - Client-side interceptor processing request "
                    + ri.operation() + "\n for thread with gid " + dtgid);
        }catch(InvalidSlot e){
            logger.error(name + " - Error! Invalid context slot specified.", e);
            return;
        }catch(BAD_OPERATION e){
            logger.debug("No id while processing operation - " + ri.operation());
            return;
        }
        
        /* Encodes as bytes */
        int dtgid_aux = dtgid;
        byte[] id = new byte[4];
        int mask = 255;
        for(int i = 0; i < 4; i++) {
            id[i] = (byte)(dtgid_aux & mask);
            dtgid_aux >>= 8;
        }
        
        ServiceContext sc = new ServiceContext(upcall_context,id);
        ri.add_request_service_context(sc, false);
        
        /* Notifies the central agent */
        /* Creating a hashmap for every request releases us from having 
         * to synchronize access to a global, shared hashmap.
         */
        Map <String, String>infoMap = new HashMap <String, String>();
        /* TODO Be more economical and encode those ints in base 32 */
        infoMap.put("dtid", fh.int2Hex(dtgid));
        infoMap.put("ltid", fh.int2Hex(ltgid));
        infoMap.put("top", fh.int2Hex(tpsiz));
        
        /* TODO Maybe we could be economical on the encoding here as well, 
         * especially if CORBA idl does not support UNICODE.
         */
        infoMap.put("op", ri.operation());
        
        logger.debug("Making event from map - " + infoMap);
        Event e = new Event(infoMap, DebuggerConstants.CLIENT_UPCALL);
        
        try{
            logger.debug("Attempting to notify the central agent.");
            if(global.syncNotify(e) == DebuggerConstants.STEPPING){
                tagger.setStepping(ltgid, true);
            }
        }catch(CommException ce){
            logger.fatal("Failed to notify the central agent of a client upcall - " +
            		" central agent's tracking state might be inconsistent.", ce);
        }
        
    }
    
    private void setForFourth(ClientRequestInfo ri){
        int dtgid, ltgid, tpsiz;
        try{
            ltgid = ri.get_slot(ltslot).extract_long();
            
            if(logger.isDebugEnabled()){
                logger.debug(name + " - Client-side interceptor receiving reply for "
                        + ri.operation() + "\n on behalf of local application thread " +
                        " globally unique id " + ltgid);
            }
            
            ServiceContext ctx = null;
            /* Now we check whether the thread is stepping or not.
             * This 'is stepping' is intentional state and not really
             * the thread's state.  
             * */
            try{
                ctx = ri.get_reply_service_context(downcall_context);
                byte [] data = ctx.context_data;
                
                logger.debug("Step service context is " + data[0]);
                
                if(data[0] == 1) tagger.setStepping(ltgid, true);
                else if(data[0] == 0) tagger.setStepping(ltgid, false);
                else{
                    logger.error("Unknown thread state reported by server-side debug" +
                            " interceptor. Behavior might be erratic.");
                }
            }catch(BAD_PARAM bp){ 
                /* Assymetric system - other side hasn't got the required
                 * interceptors installed. This means the other side hasn't
                 * got the debugger attached and therefore 'thread state' means
                 * only our state - we don't have to do anything.
                 */
            }
            
        }catch(InvalidSlot e){
            logger.error(name + " - Error! Invalid context slot specified.", e);
            return;
        }catch(BAD_OPERATION e){
            logger.debug("No id while processing operation - " + ri.operation());
            return;
        }
        
    }
    

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.InterceptorOperations#destroy()
     */
    public void destroy() {
        
    }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.InterceptorOperations#name()
     */
    public String name() {
        return name;
    }
}
