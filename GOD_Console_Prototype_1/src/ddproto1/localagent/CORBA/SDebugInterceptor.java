/*
 * Created on Sep 4, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ServerInterceptor.java
 */

package ddproto1.localagent.CORBA;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import ddproto1.localagent.client.GlobalAgentFactory;
import ddproto1.localagent.client.IGlobalAgent;

/**
 * @author giuliano
 *
 */
public class SDebugInterceptor extends LocalObject implements ServerRequestInterceptor{

    /**
     * 
     */
    private static final long serialVersionUID = 3689069564574906674L;

    private String name;
    
    private int upcall_context, downcall_context;
    private int dtslot;
    private int opslot;
    private int rmslot;
    private int stslot;
    
    private IGlobalAgent global;
    private Map infoMap = new HashMap();
    
    private static final Logger logger = Logger.getLogger(SDebugInterceptor.class);

    public SDebugInterceptor(String name, int upcall_context, int downcall_context, int dtslot, int opslot, int rmslot, int stepslot)
    	throws UnknownHostException, IOException
    {
        global = GlobalAgentFactory.getInstance().resolveReference();
        this.name = name;
        this.upcall_context = upcall_context;
        this.downcall_context = downcall_context;
        this.dtslot = dtslot;
        this.rmslot = rmslot;
        this.opslot = opslot;
    }
    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ServerRequestInterceptorOperations#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)
     */
    public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
        // TODO Auto-generated method stub
        logger.info("receive_request");
    }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ServerRequestInterceptorOperations#receive_request_service_contexts(org.omg.PortableInterceptor.ServerRequestInfo)
     */
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
        logger.info("receive_request_service_contexts");
        ServiceContext sc;
        
        try{
            /* Tries to obtain the request gid */
            sc = ri.get_request_service_context(upcall_context);
        }catch(BAD_PARAM ex){
            /*  No context information. Has been called by debug-unaware node. */
            return;
        }
        
        byte [] id = sc.context_data;
        if(id.length != 4){
            logger.error(name + "Error - encoded id does not obey the required format.");
            return;
        }
        
        int gid = 0;
        for(int i = 3; i >= 0; i--){
            gid <<= 8;
            gid |= id[i];
        }
        
        logger.debug("Received request for operation " + ri.operation()
                + " from distributed thread " + gid);
        
        Any dtany = ORB.init().create_any();
        Any opany = ORB.init().create_any();
        Any rmany = ORB.init().create_any();
        
        dtany.insert_long(gid);
        opany.insert_string(ri.operation());
        rmany.insert_boolean(true);
        
        try{
            ri.set_slot(dtslot, dtany);
            ri.set_slot(opslot, opany);
            ri.set_slot(rmslot, rmany);
        }catch(InvalidSlot e){
            logger.error(name + " - Cannot insert info into slot.", e);
        }
        
    }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ServerRequestInterceptorOperations#send_exception(org.omg.PortableInterceptor.ServerRequestInfo)
     */
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest { this.intoService(ri); }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ServerRequestInterceptorOperations#send_other(org.omg.PortableInterceptor.ServerRequestInfo)
     */
    public void send_other(ServerRequestInfo ri) throws ForwardRequest { this.intoService(ri); }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.ServerRequestInterceptorOperations#send_reply(org.omg.PortableInterceptor.ServerRequestInfo)
     */
    public void send_reply(ServerRequestInfo ri) { this.intoService(ri); }
    
    private void intoService(ServerRequestInfo ri){
        try{
            Any stepInfo = ri.get_slot(stslot);
            boolean isStepping = stepInfo.extract_boolean();
            byte context [] = new byte[1];
            context[0] = (byte)((isStepping)?1:0);
            
            ServiceContext stepContext = new ServiceContext();
            stepContext.context_data = context;
            stepContext.context_id = downcall_context;
            
            ri.add_reply_service_context(stepContext, false);
        }catch(InvalidSlot e){
            logger.error("Failed to acquire the stepping context - breakpoint returns " +
                    "may not work properly.");
        }catch(BAD_OPERATION e){
            logger.error("Failed to extract the correct type from the slot assigned to conveying step information." +
                    " Debugger might not operate correctly.");
        }
    }
    
    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.InterceptorOperations#destroy()
     */
    public void destroy() { }

    /* (non-Javadoc)
     * @see org.omg.PortableInterceptor.InterceptorOperations#name()
     */
    public String name() {
        return name;
    }

}
