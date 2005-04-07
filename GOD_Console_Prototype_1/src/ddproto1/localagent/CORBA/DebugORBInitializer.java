/*
 * Created on Sep 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DebugORBInitializer.java
 */

package ddproto1.localagent.CORBA;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;

import ddproto1.localagent.Tagger;

/**
 * @author giuliano
 *  
 */
public class DebugORBInitializer extends LocalObject implements ORBInitializer {

    /**
     * 
     */
    private static final long serialVersionUID = 3546078068776186423L;
    private static int upcallId = 3194;
    private static int downcallId = 3195;

    /*
     * (non-Javadoc)
     * 
     * @see org.omg.PortableInterceptor.ORBInitializerOperations#post_init(org.omg.PortableInterceptor.ORBInitInfo)
     */
    public void post_init(ORBInitInfo info) {

        org.omg.CORBA.Object obj;
        Logger logger = Logger.getLogger("agent.local");
        try {
            obj = info.resolve_initial_references("PICurrent");
        } catch (InvalidName e) {
            logger.fatal(e.toString(), e);
            return;
        }

        org.omg.PortableInterceptor.Current current = org.omg.PortableInterceptor.CurrentHelper
                .narrow(obj);
        assert (current != null);
        
        /* Allocates our six slots */
        int dtslotty = info.allocate_slot_id(); // Current distributed thread id
        int idslotty = info.allocate_slot_id(); // Current local thread id (servicing request or upcalling)
        int opslotty = info.allocate_slot_id(); // Current operation name
        int rmslotty = info.allocate_slot_id(); // Remote-or-local call information slot
        int tpslotty = info.allocate_slot_id(); // Top-of-the-stack slot.
        int stslotty = info.allocate_slot_id(); // Stepping info slot.

        try{
            ORBHolder ch = ORBHolder.getInstance();
            ch.registerPICurrent(current, dtslotty, idslotty, opslotty, rmslotty, tpslotty, stslotty);

            /* Now registers our interceptors */
            int gid = Tagger.getInstance().getGID();

            String iname = "Debug Interceptor (at node " + gid + ")";
            CDebugInterceptor cdbi = new CDebugInterceptor("Client " + iname,
                    upcallId, downcallId, dtslotty, idslotty, tpslotty);
            SDebugInterceptor sdbi = new SDebugInterceptor("Server " + iname,
                    upcallId, downcallId, dtslotty, opslotty, rmslotty, stslotty);

            info.add_client_request_interceptor(cdbi);
            info.add_server_request_interceptor(sdbi);
            
        } catch (DuplicateName e) {
            logger.warn("Warning - debug interceptor already installed "
                    + "(There's probably something very wrong going on). ");
        } catch(UnknownHostException e){
            logger.fatal("Could not find the global agent host at the specified " +
            		"address (maybe the server is down?).",e);
        } catch(IOException e){
            logger.fatal("I/O exception while installing interceptors.", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.omg.PortableInterceptor.ORBInitializerOperations#pre_init(org.omg.PortableInterceptor.ORBInitInfo)
     */
    public void pre_init(ORBInitInfo info) {
    }

}