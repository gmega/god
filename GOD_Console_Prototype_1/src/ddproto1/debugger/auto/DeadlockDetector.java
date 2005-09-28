/*
 * Created on Sep 26, 2005
 * 
 * file: DeadlockDetector.java
 */
package ddproto1.debugger.auto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.alg.StrongConnectivityInspector;
import org._3pq.jgrapht.graph.DirectedMultigraph;
import org._3pq.jgrapht.graph.DirectedSubgraph;
import org._3pq.jgrapht.traverse.DepthFirstIterator;
import org.apache.log4j.Logger;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.debugger.managing.tracker.DistributedThread;
import ddproto1.debugger.managing.tracker.VirtualStackframe;
import ddproto1.debugger.managing.tracker.DistributedThread.VirtualStack;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.UnorderedMultiMap;
import ddproto1.util.traits.commons.ConversionTrait;

public class DeadlockDetector {
    
    private static MessageHandler mh = MessageHandler.getInstance();
    private static VMManagerFactory vmmf = VMManagerFactory.getInstance();
    private static ConversionTrait ct = ConversionTrait.getInstance();
    
    private Set<DistributedThread> suspendedDTs;
    private Set<ThreadReference> suspendedLTs;

    
    public void detect(Set<DistributedThread> dtSet, Set<ThreadReference> ltSet){
        try{
            suspendedDTs = new HashSet<DistributedThread>();
            suspendedLTs = new HashSet<ThreadReference>();
            mh.getStandardOutput().print("Suspending local threads ...");
            mh.getStandardOutput().println("[ok]");


            this.suspendAll(dtSet, ltSet);
        
            mh.getStandardOutput().print("Snapshotting monitor state...");
            Map <ObjectReference, Object> monOwners  = new HashMap<ObjectReference, Object>();
            UnorderedMultiMap <ObjectReference, Object> monWaiters = 
                new UnorderedMultiMap<ObjectReference, Object>(HashSet.class);
            for(DistributedThread dt : dtSet){
                Set <ThreadReference> components = expandToThreads(dt, true);
                for(ObjectReference ownedMonitor : ownedMonitors(components))
                    monOwners.put(ownedMonitor, dt);
                for(ObjectReference contendedMonitor : contendedMonitors(components, false))
                    monWaiters.add(contendedMonitor, dt);
            }
            
            for(ThreadReference tr : ltSet){
                for(ObjectReference ownedMonitor : tr.ownedMonitors())
                    monOwners.put(ownedMonitor, tr);
                
                ObjectReference contendedMonitor = tr.currentContendedMonitor();
                /** This limits the kinds of deadlocks we can detect, but the ones we 
                 * detect are both more common and easier to catch.
                 */
                if (contendedMonitor != null
                        && tr.status() == ThreadReference.THREAD_STATUS_MONITOR)
                    monWaiters.add(contendedMonitor, tr);
            }
            mh.getStandardOutput().println("[ok]");
            
            mh.getStandardOutput().print("Building dependency graph...");
            DirectedGraph graph = this.buildGraph(dtSet, ltSet, monOwners, monWaiters);
            mh.getStandardOutput().println("[ok]");
            
            mh.getStandardOutput().print("Analyzing dependency graph...");
            StrongConnectivityInspector sci = new StrongConnectivityInspector(graph);
            List<DirectedSubgraph> subgraphs = sci.stronglyConnectedSubgraphs();
            
            /** I pre-process the subgraph list to remove the vertices with no self-edges. */
            for(Iterator<DirectedSubgraph> it = subgraphs.iterator(); it.hasNext();){
                DirectedSubgraph subgraph = it.next();
                if(subgraph.vertexSet().size() == 1 && subgraph.edgeSet().size() == 0)
                    it.remove();
            }
            
            mh.getStandardOutput().println("[ok]");
            
            
            mh.getStandardOutput().println("Analysis results:");
            if(subgraphs.isEmpty())
                mh.getStandardOutput().println("- No distributed deadlocks found.");
            else{
                mh.getStandardOutput().println("- Distributed deadlocks found. Deadlocks are shown below:");
                int i = 0;
                String firstThread = null;                
                for(DirectedSubgraph deadlockGraph : subgraphs){
                    mh.getStandardOutput().println("Deadlock #" + ++i + ":");
                    DepthFirstIterator dfi = new DepthFirstIterator(deadlockGraph);
                    while(dfi.hasNext()){
                        Object vertex = dfi.next();
                        String currentUUID;
                        if(vertex instanceof ThreadReference){
                            ThreadReference tr = (ThreadReference)vertex;
                            VirtualMachineManager vmm = vmmf.getVMManager(vmmf.getGidByVM(tr.virtualMachine()));
                            currentUUID = ct.uuid2Dotted(vmm.getThreadManager().getThreadUUID(tr));
                        }else if(vertex instanceof DistributedThread){
                            DistributedThread dt = (DistributedThread)vertex;
                            currentUUID = ct.uuid2Dotted(dt.getId());
                        }else{
                            currentUUID = "<error>";
                        }
                        
                        if(firstThread != null) mh.getStandardOutput().println(currentUUID);
                        mh.getStandardOutput().print("Distributed thread " + currentUUID + " waits for thread ");
                        if(firstThread == null) firstThread = currentUUID;
                    }
                    mh.getStandardOutput().println(firstThread);
                }
            }
        }catch(Throwable t){
            mh.getErrorOutput().println("Error while attempting to detect distributed deadlocks.");
            mh.printStackTrace(t);
        }finally{
            this.resumeSuspended();
        }
    }
    
    private DirectedGraph buildGraph(Set<DistributedThread> dtSet,
            Set<ThreadReference> ltSet, Map<ObjectReference, Object> monOwners,
            UnorderedMultiMap<ObjectReference, Object> monWaiters) {
        DirectedMultigraph graph = new DirectedMultigraph();
        graph.addAllVertices(dtSet);
        graph.addAllVertices(ltSet);
        
        for(ObjectReference contendedMonitor : monWaiters.keySet()){
            Object owner = monOwners.get(contendedMonitor);
            if(owner == null){
                mh.getWarningOutput().println("Thread contends on monitor of unspecified owner.");
                continue;
            }
            for(Object waiter : monWaiters.getClass(contendedMonitor))
                graph.addEdge(waiter, owner);
        }
        return graph;
    }
    
    private List<ObjectReference> ownedMonitors(Set<ThreadReference> threads)
        throws IncompatibleThreadStateException
    {
        List<ObjectReference> monList = new ArrayList<ObjectReference>();
        for(ThreadReference tr : threads)
            monList.addAll(tr.ownedMonitors());
        
        return monList;
    }
    
    private List<ObjectReference> contendedMonitors(Set<ThreadReference> threads, boolean includeWaiting)
        throws IncompatibleThreadStateException
    {
        List<ObjectReference> monList = new ArrayList<ObjectReference>();
        for(ThreadReference tr : threads){
            if (tr.status() == ThreadReference.THREAD_STATUS_MONITOR)
                monList.add(tr.currentContendedMonitor());
            else if (tr.status() == ThreadReference.THREAD_STATUS_WAIT
                    && includeWaiting)
                monList.add(tr.currentContendedMonitor());
        }
        return monList;
    }
    
    private Set<ThreadReference> expandToThreads (DistributedThread dt, boolean includeHead){
        Set<ThreadReference> allThreads = new HashSet<ThreadReference>();
        VirtualStack vs = dt.virtualStack();
        int frames = vs.getVirtualFrameCount();
        for(int i = (includeHead)?0:1; i < frames; i++){
            VirtualStackframe vsf = vs.getVirtualFrame(i);
            VirtualMachineManager vmm = vmmf.getVMManager(vsf.getLocalThreadNodeGID());
            ThreadReference tr = vmm.getThreadManager().findThreadByUUID(vsf.getLocalThreadId());
            assert tr != null;
            assert !allThreads.contains(tr);
            allThreads.add(tr);
        }
        
        return allThreads;
    }

    private void suspendAll(Set<DistributedThread> dtSet,
            Set<ThreadReference> ltSet) {
        Set<ThreadReference> locals = new HashSet(ltSet);
        /** Suspends all distributed threads. */
        for (DistributedThread dt : dtSet) {
            try{
                dt.lock();
                while (!dt.isSuspended())
                    dt.suspend();
            }finally{
                dt.unlock();
            }
            suspendedDTs.add(dt);
            Set<ThreadReference> trs = expandToThreads(dt, true);

            /**
             * The local thread set and the distributed thread set should be
             * disjoint.
             */
            for (ThreadReference tr : trs)
                assert !ltSet.contains(tr);

            /** Add all threads to the ltSet, including the head. */
            locals.addAll(trs);
        }

        /** Suspends all local threads. */
        for (ThreadReference tr : locals) {
            while (!tr.isSuspended())
                tr.suspend();
            suspendedLTs.add(tr);
        }

    }
    
    private void resumeSuspended(){
        try{
            /** Since the head might have been added to the ltSet, we must
             * resume the distributed threads first.
             */
            for(DistributedThread dt : suspendedDTs){
                try {
                    dt.lock();
                    if (dt.isSuspended()) 
                        dt.resume();
                } finally {
                    dt.unlock();
                }
            }
        }finally{
            for(ThreadReference tr : suspendedLTs)
                if(tr.isSuspended()) tr.resume();
        }
    }
}
