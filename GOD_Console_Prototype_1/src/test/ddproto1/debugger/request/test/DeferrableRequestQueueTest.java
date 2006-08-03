/*
 * Created on Mar 23, 2005
 * 
 * file: DeferrableRequestQueueTest.java
 */
package ddproto1.debugger.request.test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ddproto1.debugger.request.DeferrableRequestQueue;
import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.debugger.request.IResolutionListener;
import ddproto1.debugger.request.StdPreconditionImpl;
import ddproto1.debugger.request.StdResolutionContextImpl;
import ddproto1.debugger.request.StdTypeImpl;
import ddproto1.debugger.request.IDeferrableRequest.IPrecondition;
import junit.framework.TestCase;

public class DeferrableRequestQueueTest extends TestCase {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(DeferrableRequestQueueTest.class);
    }

    /*
     * Class under test for boolean addEagerlyResolve(IDeferrableRequest)
     */
    public void testAddEagerlyResolveIDeferrableRequest() 
        throws Exception
    {
        DeferrableRequestQueue drq = new DeferrableRequestQueue("Test VM");
        
        /* Adds some stuff to resolve eagerly. We smuggle some information (namely the name of the
         * class that will resolve the precondition) into the precondition, but this information is
         * normally not known beforehand - this is just a test. */
        PatternPrecondition pp1 = new PatternPrecondition("ddproto1.Shinari");
        pp1.setClassId("ddproto1.*");
        pp1.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_MULTIPLE));
        
        PatternPrecondition pp2 = new PatternPrecondition("ddproto1.Harkonnen");
        pp2.setClassId("ddproto1.*");
        pp2.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_MULTIPLE));
        
        List<IPrecondition> reqs = new LinkedList<IPrecondition>();
        reqs.add(pp1);
        reqs.add(pp2);
        
        IDeferrableRequest idr = new Request(reqs);
        
        drq.addEagerlyResolve(idr);
        
        /* Meets the preconditions. */
        StdResolutionContextImpl cti_shinari = new StdResolutionContextImpl();
        StdResolutionContextImpl cti_atreides = new StdResolutionContextImpl();
        StdResolutionContextImpl cti_harkonnen = new StdResolutionContextImpl();
        
        
        // Real
        StdPreconditionImpl shinari = new StdPreconditionImpl();
        shinari.setClassId("ddproto1.Shinari");
        shinari.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        cti_shinari.setPrecondition(shinari);
        
        // Decoy
        StdPreconditionImpl atreides = new StdPreconditionImpl();
        atreides.setClassId("ddproto1.Atreides");
        atreides.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        cti_atreides.setPrecondition(atreides);
        
        // Real
        StdPreconditionImpl harkonnen = new StdPreconditionImpl();
        harkonnen.setClassId("ddproto1.Harkonnen");
        harkonnen.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, IDeferrableRequest.MATCH_ONCE));
        cti_harkonnen.setPrecondition(harkonnen);
        
        drq.resolveForContext(cti_shinari);
        drq.resolveForContext(cti_atreides);
        drq.resolveForContext(cti_harkonnen);
                
    }

    public void testResolveForContext() {
        //TODO Implement resolveForContext().
        System.out.println("2");
    }
    
    class Request implements IDeferrableRequest{
        
        private List<IPrecondition> reqs, clone;
        
        public Request(List<IPrecondition> requirements){
            reqs = requirements;
            clone = new LinkedList<IPrecondition>(reqs);
        }
        
        public Object resolveNow(IResolutionContext context) throws Exception {
            boolean resolved = false;
            
            PatternPrecondition ts = null;
            for(Iterator it = reqs.iterator(); it.hasNext();){
                ts = (PatternPrecondition)it.next();
                if(ts.getResolve().equals(context.getPrecondition().getClassId())){
                    resolved |= true;
                    it.remove();
                }
            }
            
            if(resolved) return new Object();
            return null;
        }

        public void addResolutionListener(IResolutionListener listener) { }

        public void removeResolutionListener(IResolutionListener listener) { }

        public List<IPrecondition> getRequirements() { 
            return clone;
        }

        public void cancel() throws Exception {
            // TODO Auto-generated method stub
            
        }

        public boolean isCancelled() {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    class PatternPrecondition extends StdPreconditionImpl{
        private String trueResolve;
        
        public PatternPrecondition(String trueResolve){
            this.trueResolve = trueResolve;
        }
        
        public String getResolve(){
            return trueResolve;
        }
    }

}
