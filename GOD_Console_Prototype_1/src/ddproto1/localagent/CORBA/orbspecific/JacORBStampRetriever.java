/*
 * Created on Sep 20, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: JacORBStampRetriever.java
 */

package ddproto1.localagent.CORBA.orbspecific;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.PortableInterceptor.InvalidSlot;

import ddproto1.exception.commons.UnsupportedException;
import ddproto1.localagent.Tagger;
import ddproto1.localagent.PIManagementDelegate.CurrentSpec;
import ddproto1.util.traits.commons.ConversionTrait;

/**
 * @author giuliano
 *
 */
public class JacORBStampRetriever implements IStampRetrievalStrategy {
    
    private static Logger logger = Logger.getLogger("agent.local");

    /* (non-Javadoc)
     * @see ddproto1.localagent.CORBA.orbspecific.IStampRetrievalStrategy#retrieveStamp(java.util.Iterator)
     */
    public String retrieve(String what, Iterator currentSpecs) throws Exception{
        // TODO Auto-generated method stub
        Iterator it = currentSpecs;
        CurrentSpec cs;

        /* Experience has shown that JacORB somehow manages
         * to keep id consistency among all Thread PICurrents, even if
         * they belong to different ORBs. It would be reasonable
         * to assume that JacORB uses a ThreadLocal that is shared
         * among all org.omg.CORBA.ORB instances.
         * 
         * Since JacORB works like that, all we need is the first PICurrent
         * on the list. 
         */

        cs = (CurrentSpec) it.next();
        
        String result = null;
        
        try {
            if (what.equals("stamp")) {
                ConversionTrait fh = ConversionTrait.getInstance();
                Any any = cs.getCurrent().get_slot(cs.getDTSlot());
                result = fh.int2Hex(any.extract_long());
                
            } else if (what.equals("opname")) {
                
                Any any = cs.getCurrent().get_slot(cs.getOPSlot());
                result = any.extract_string();

            } else if(what.equals("locality")) {
                try{
                    Any any = cs.getCurrent().get_slot(cs.getREMSlot());
                    return String.valueOf(any.extract_boolean());
                    /* This is the only case where an exception has a meaning
                     * other than failure - it might just mean the call is a local
                     * call.
                     */
                }catch(BAD_OPERATION e){
                    return "false";
                }
            
        	}else if (what.equals("step_stats")){
                try{
                    
                }catch(BAD_OPERATION e){
                    
                }
            }else {
                throw new UnsupportedException(
                        "Could not service request for unknown "
                                + "tagging component " + what);
            }
        } catch (InvalidSlot e) {
            logger.error(
                    "Could not acquire slot for extracting Distributed Thread"
                            + " GUID.", e);
        } catch (BAD_OPERATION e) {
            logger
                    .warn("Failed to establish context information for "
                            + "a called method. Maybe your skeleton method list is outdated?");
        }
        
        return result;
    }
    
}
