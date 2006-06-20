/*
 * Created on Jun 19, 2006
 * 
 * file: LocalMultiDeathTest.java
 */
package ddproto1.controller.remote.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import Ice.AMI_Object_ice_invoke;
import Ice.ByteSeqHolder;
import Ice.Communicator;
import Ice.Connection;
import Ice.Endpoint;
import Ice.Identity;
import Ice.LocatorPrx;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;
import Ice.OperationMode;
import Ice.RouterPrx;
import ddproto1.controller.client.AMI_ControlClient_notifyProcessDeath;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDERR;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDOUT;
import ddproto1.controller.client.LaunchParameters;
import ddproto1.controller.remote.ProcessServerPrx;
import ddproto1.controller.remote.RemoteProcessPrx;
import ddproto1.controller.remote.impl.ProcessServerImpl;
import ddproto1.controller.remote.impl.RemoteProcessImpl;

/**
 * 
 * This test is now broken because it's too difficult to simulate all
 * interactions with the ICE stuff I had to insert into the classes.
 * 
 * Run ICEMultiDeathTest instead.
 * 
 * @author giuliano
 *
 */
public class LocalMultiDeathTest extends MultiDeathTest{
    
    private class HollowControlClient implements ControlClientExt{
        
        private ControlClientOps ccOps;
        
        public HollowControlClient(int nProcs) {
            setOps(new ControlClientOps(nProcs));
        }
        
        protected synchronized void setOps(ControlClientOps ccOps){
            this.ccOps = ccOps;
        }
        
        protected synchronized ControlClientOps getOps(){
            return ccOps;
        }

        public void receiveStringFromSTDOUT_async(AMI_ControlClient_receiveStringFromSTDOUT __cb, int pHandle, String data) {
            getOps().receiveStringFromSTDOUT_async(__cb, pHandle, data);
        }

        public void receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data) {
            getOps().receiveStringFromSTDERR_async(__cb, pHandle, data);
        }
        
        public void notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle) {
            getOps().notifyProcessDeath_async(__cb, pHandle);
            notifyDeath();
        }
        
        public boolean isDone(){
            return getOps().isDone();
        }
        
        // Huge ammount of no-op methods. These belong to the ICE interfaces and we don't use them.
        public void notifyProcessDeath(int pHandle) { }
        public void notifyProcessDeath(int pHandle, Map __ctx) { }
        public void notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle, Map __ctx) { }
        public void receiveStringFromSTDOUT(int pHandle, String data) { }
        public void receiveStringFromSTDOUT(int pHandle, String data, Map mup) { }
        public void receiveStringFromSTDOUT_async(AMI_ControlClient_receiveStringFromSTDOUT __cb, int pHandle, String data, Map __ctx) { }
        public void receiveStringFromSTDERR(int pHandle, String data) { }
        public void receiveStringFromSTDERR(int pHandle, String data, Map __ctx) { }
        public void receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data, Map __ctx) { }
        public int ice_hash() { return 0; }
        public Communicator ice_communicator() { return null; }
        public String ice_toString() { return null; }
        public boolean ice_isA(String arg0) { return false; }
        public boolean ice_isA(String arg0, Map arg1) { return false; }
        public void ice_ping() { }
        public void ice_ping(Map arg0) { }
        public String[] ice_ids() { return null; }
        public String[] ice_ids(Map arg0) { return null; }
        public String ice_id() { return null; }
        public String ice_id(Map arg0) { return null; }
        public boolean ice_invoke(String arg0, OperationMode arg1, byte[] arg2, ByteSeqHolder arg3) { return false; }
        public boolean ice_invoke(String arg0, OperationMode arg1, byte[] arg2, ByteSeqHolder arg3, Map arg4) { return false; }
        public void ice_invoke_async(AMI_Object_ice_invoke arg0, String arg1, OperationMode arg2, byte[] arg3) {}
        public void ice_invoke_async(AMI_Object_ice_invoke arg0, String arg1, OperationMode arg2, byte[] arg3, Map arg4) { }
        public Identity ice_getIdentity() { return null; }
        public ObjectPrx ice_newIdentity(Identity arg0) { return null; }
        public Map ice_getContext() { return null; }
        public ObjectPrx ice_newContext(Map arg0) { return null; }
        public ObjectPrx ice_defaultContext() { return null; }
        public String ice_getFacet() { return null; }
        public ObjectPrx ice_newFacet(String arg0) { return null; }
        public String ice_getAdapterId() { return null; }
        public ObjectPrx ice_newAdapterId(String arg0) { return null; }
        public Endpoint[] ice_getEndpoints() { return null; }
        public ObjectPrx ice_newEndpoints(Endpoint[] arg0) { return null; }
        public ObjectPrx ice_twoway() { return null; }
        public boolean ice_isTwoway() { return false; }
        public ObjectPrx ice_oneway() { return null; }
        public boolean ice_isOneway() { return false; }
        public ObjectPrx ice_batchOneway() { return null; }
        public boolean ice_isBatchOneway() { return false; }
        public ObjectPrx ice_datagram() { return null; }
        public boolean ice_isDatagram() { return false; }
        public ObjectPrx ice_batchDatagram() { return null; }
        public boolean ice_isBatchDatagram() { return false; }
        public ObjectPrx ice_secure(boolean arg0) { return null; }
        public ObjectPrx ice_compress(boolean arg0) { return null; }
        public ObjectPrx ice_timeout(int arg0) { return null; }
        public ObjectPrx ice_connectionId(String arg0) { return null; }
        public ObjectPrx ice_router(RouterPrx arg0) { return null; }
        public ObjectPrx ice_locator(LocatorPrx arg0) { return null; }
        public ObjectPrx ice_collocationOptimization(boolean arg0) { return null; }
        public Connection ice_connection() { return null; }

        public void notifyServerUp(ProcessServerPrx procServer) {
            // TODO Auto-generated method stub
            
        }

        public void notifyServerUp(ProcessServerPrx procServer, Map __ctx) {
            // TODO Auto-generated method stub
            
        }

    }
    
    private static final int NPROCS = 10;
    
    public void setUp(){
        super.setUp();
        nprocs = NPROCS;
        ControlClientExt ccExt = new HollowControlClient(NPROCS);
        setGlobalAgent(ccExt);
        setPServerImpl(adaptPSToPrx(new ProcessServerImpl(ccExt, 
                (ObjectAdapter)Proxy.newProxyInstance(this.getClass().getClassLoader(), 
                        new Class [] {ObjectAdapter.class}, new InvocationHandler(){
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                return null;
                            }
                }))));
    }

    @Override
    protected RemoteProcessPrx castToPrx(Object prx) {
        final RemoteProcessImpl pImpl = (RemoteProcessImpl) prx;
        RemoteProcessPrx pprx = (RemoteProcessPrx) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class [] { RemoteProcessPrx.class }, new InvocationHandler(){
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object retVal = null;
                        if(method.getName().equals("isAlive")){
                            retVal = pImpl.isAlive();
                        }else if(method.getName().equals("dispose")){
                            pImpl.dispose();
                        }else if(method.getName().equals("getHandle")){
                            retVal = pImpl.getHandle();
                        }else{
                            throw new UnsupportedOperationException();
                        }
                        return retVal;
                    }
        });
        
        return pprx;
    }

    protected ProcessServerPrx adaptPSToPrx(final ProcessServerImpl impl){
        ProcessServerPrx pprx = (ProcessServerPrx) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class [] { ProcessServerPrx.class }, new InvocationHandler(){

                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if(method.getName().equals("launch")){
                            return impl.launch((LaunchParameters)args[0]);
                        }else if(method.getName().equals("getProcessList")){
                            return impl.getProcessList();
                        }else
                            throw new UnsupportedOperationException();
                    }
            
        });
        
        return pprx;
    }        

}
