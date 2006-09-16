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
import ddproto1.exception.commons.CommException;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;
import ddproto1.util.commons.Event;
import ddproto1.util.traits.commons.ConversionUtil;

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
    private ConversionUtil fh = ConversionUtil.getInstance();
    
    private static final Logger requestLogger = Logger.getLogger(CDebugInterceptor.class.getName() + ".requestLogger");
    
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
        if(requestLogger.isDebugEnabled())
            requestLogger.debug("Entering send_request for operation " + ri.operation());
                        
        /* Obtains the gid for the current distributed thread */
        int dtgid, ltgid, tpsiz;
        try{
            dtgid = ri.get_slot(dtslot).extract_long();
            ltgid = ri.get_slot(ltslot).extract_long();
            tpsiz = ri.get_slot(tpslot).extract_long();
            requestLogger.debug(name + " - Client-side interceptor processing request "
                    + ri.operation() + "\n for thread with gid " + dtgid);
        }catch(InvalidSlot e){
            requestLogger.error(name + " - Error! Invalid context slot specified.", e);
            return;
        }catch(BAD_OPERATION e){
            requestLogger.debug("No id while processing operation - " + ri.operation());
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
        
        Event e = new Event(infoMap, DebuggerConstants.CLIENT_UPCALL);
        
        StringBuffer debugBuffer = new StringBuffer();
        try{
            
            
            if(requestLogger.isDebugEnabled()){
                debugBuffer.append( "\n  Remote operation: " + ri.operation() + 
                                    "\n  Distributed thread ID: " + fh.uuid2Dotted(dtgid) +  
                                    "\n  Local thread: " + fh.uuid2Dotted(ltgid));
            }

            byte stats = global.syncNotify(e);
            
            if(requestLogger.isDebugEnabled()){
                requestLogger.debug("Notified local agent of CLIENT_UPCALL." +
                        debugBuffer.toString() +
                        "\n Thread state: " + fh.statusText(stats));
            }
            
            /** Do I really have to set the local thread to step mode 
             * when doing the upcall? I don't think so. 
             * I'm removing the code below for now and setting it to the return
             * part.
             * 
             * NOTE: I have to, because the other side might not be under the debuggers
             * realm. If I don't do this:
             * 
             * 1) The component boundary recognizer resumes the thread.
             * 2) The STEPPING_REMOTE information is lost.
             * 3) We receive reply and #setForFourth won't be able to retrieve the 
             *    current step status.
             * 4) The instrumented code at the stub won't trap the thread and it won't
             *    stop.
             *    
             * Result: The user does a step into and his/her thread simply resumes. 
             */  

            if((stats&DebuggerConstants.STEPPING) != 0){
                tagger.setStepping();
            }else{
                tagger.unsetStepping();
            }
            
             
 
        }catch(CommException ce){
            requestLogger.fatal("Failed to notify the central agent of a client upcall - " +
            		" central agent's tracking state might be inconsistent.", ce);
        }
        
    }
    
    private void setForFourth(ClientRequestInfo ri){
        int ltgid;
        StringBuffer debugBuffer = new StringBuffer();
        if(requestLogger.isDebugEnabled()){
            debugBuffer.append(name);
            debugBuffer.append(" - Client-side interceptor receiving reply for ");
            debugBuffer.append(ri.operation());

        }
        try{
        	/* Plain wrong. The local thread to be stopped might not be
        	 * the base thread. So the line below is wrong.
        	 * 
        	 * ltgid = ri.get_slot(ltslot).extract_long();
        	 * 
        	 * This line should be used for retrieving information about
        	 * the currently assigned local thread. 
        	 */
        	int assertltgid = ri.get_slot(ltslot).extract_long();
        	ltgid = Tagger.getInstance().currentTag();
            if(requestLogger.isDebugEnabled()){
                debugBuffer.append("\n on behalf of local application thread ");
                debugBuffer.append(" globally unique id ");
                debugBuffer.append(fh.uuid2Dotted(ltgid));
            }
            
            ServiceContext ctx = null;
            /* Now we check whether the thread is stepping or not.
             * This 'is stepping' is intentional state and not really
             * the thread's state.  
             * */
            try{
                ctx = ri.get_reply_service_context(downcall_context);
                byte [] data = ctx.context_data;
                
                if(requestLogger.isDebugEnabled()){
                	debugBuffer.append("\n Local Thread ID: " + fh.uuid2Dotted(ltgid));
                	debugBuffer.append("\n Status: " + fh.statusText(data[0])) ;
                }
                                
                if(/*(data[0] & DebuggerConstants.ILLUSION) != 0 &&*/
                		(data[0] & DebuggerConstants.STEPPING) != 0){
                	if(requestLogger.isDebugEnabled()){
                		debugBuffer.append("Marked thread for halt. ");
                	}
                    tagger.setStepping();
                }else{
                    tagger.unsetStepping();
                }
            }catch(BAD_PARAM bp){ 

            	debugBuffer.append("Other side hasn't got debug interceptors installed");
                /* Assymetric system - other side hasn't got the required
                 * interceptors installed. This means the other side hasn't
                 * got the debugger attached and therefore 'thread state' means
                 * only our state - we don't have to do anything.
                 */
            }
        }catch(InvalidSlot ex){
        	requestLogger.error("No distributed thread associated with current request. ");
        }catch(BAD_OPERATION e){
        	if(requestLogger.isDebugEnabled()) requestLogger.debug(debugBuffer.toString());
            requestLogger.debug("No id while processing operation - " + ri.operation());
            return;
        }
        if(requestLogger.isDebugEnabled()) requestLogger.debug(debugBuffer.toString());
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
