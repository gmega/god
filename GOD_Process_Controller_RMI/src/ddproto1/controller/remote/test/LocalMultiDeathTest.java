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
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.remote.ProcessServerPrx;
import ddproto1.controller.remote.RemoteProcessPrx;
import ddproto1.controller.remote.impl.RemoteProcessServerImpl;
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

        public void receiveStringFromSTDOUT(int pHandle, String data) {
            getOps().receiveStringFromSTDOUT(pHandle, data);
        }

        public void receiveStringFromSTDERR(int pHandle, String data) {
            getOps().receiveStringFromSTDERR(pHandle, data);
        }
        
        public void notifyProcessDeath(int pHandle) {
            getOps().notifyProcessDeath(pHandle);
            notifyDeath();
        }
        
        public void notifyServerUp(IProcessServer pServer){
            
        }
        
        public boolean isDone(){
            return getOps().isDone();
        }
    }
    
    private static final int NPROCS = 10;
    
    public void setUp(){
        super.setUp();
        nprocs = NPROCS;
        ControlClientExt ccExt = new HollowControlClient(NPROCS);
        setGlobalAgent(ccExt);
        setPServerImpl(adaptPSToPrx(new RemoteProcessServerImpl(ccExt, 
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

    protected ProcessServerPrx adaptPSToPrx(final RemoteProcessServerImpl impl){
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
