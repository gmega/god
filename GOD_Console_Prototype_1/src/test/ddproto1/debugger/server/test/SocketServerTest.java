/*
 * Created on Dec 3, 2005
 * 
 * file: SocketServerTest.java
 */
package ddproto1.debugger.server.test;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.server.IRequestHandler;
import ddproto1.debugger.server.SocketServer;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.ParserException;
import ddproto1.localagent.client.GlobalAgentProxyImpl;
import ddproto1.localagent.client.IGlobalAgent;
import ddproto1.localagent.client.PooledConnectionManager;
import ddproto1.util.ILogManager;
import ddproto1.util.MessageHandler;
import ddproto1.util.commons.ByteMessage;
import ddproto1.util.commons.Event;
import ddproto1.localagent.client.*;
import ddproto1.localagent.client.PooledConnectionManager.WaitUntilAvailablePolicy;
import junit.framework.TestCase;

/**
 * This test will create multiple clients. Those clients will in turn 
 * generate multiple connections. We then deactivate the server, after 
 * some of the clients have completed their actions.
 * 
 * @author giuliano
 *
 */
public class SocketServerTest extends TestCase {

    private static final int PORT = 8080;
    private static final byte N_CLIENTS = 30;
    
    private static AtomicInteger nPrints = new AtomicInteger(0);
    
    
    SocketServer ss;
    
    public SocketServerTest(){
        super();
    }
    
    public void setUp() throws Exception {
        super.setUp();
        
        initializeMessageHandler();
        
        ss = new SocketServer();
        ss.setMaxConnections(N_CLIENTS);
        ss.setMaxThreads(N_CLIENTS);
        ss.setPort(PORT);
        ss.start();
    }
    
    public void testSetHandlers(){
        try{
            
            Thread [] ts = assembleClients(false);
            for(byte i = 0; i < N_CLIENTS; i++)
                ss.registerHandlerForNode((byte)i, new Handler((byte)i, 100, false));

            doRun(ts);
            System.out.println("Computed prints: " + nPrints.get());
            
            ts = assembleClients(true);
            ss.setSingletonHandler(new Handler((byte)0, 100, true));
            nPrints.set(0);
            doRun(ts);
            
            System.out.println("Computed prints: " + nPrints.get());

        }catch(Exception ex){ ex.printStackTrace(); fail(); }
    }
    
    protected Thread[] assembleClients(boolean sing){
        Thread ts[] = new Thread[N_CLIENTS];
        
        for(byte i = 0; i < N_CLIENTS; i++){
            Client _c = new Client((byte)i, sing);
            ts[i] = new Thread(_c);
            ts[i].setName("Client thread - " + i);
        }
        
        return ts;
    }
    
    protected void doRun(Thread[] ts) throws Exception{
        for(byte i = 0; i < N_CLIENTS; i++){
            ts[i].start();
        }
        
        for(byte i = 0; i < N_CLIENTS; i++){
            ts[i].join();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private void initializeMessageHandler(){
        BasicConfigurator.configure();
        MessageHandler mh = MessageHandler.getInstance();
        mh.setLogManagerDelegate(new ILogManager(){
            public Logger getLogger(Class c) { return Logger.getLogger(c); }
            public Logger getLogger(String name) { return Logger.getLogger(name); }
        });
        
        Logger.getRootLogger().setLevel(Level.ERROR);
        Logger.getLogger("ddproto1.debugger.server").setLevel(Level.DEBUG);
    }
    
    private class Handler implements IRequestHandler{
        
        private Byte gid;
        private long wait = 0;
        private boolean singleton;
        
        public Handler(Byte gid, long waitState, boolean singletonHandler){
            this.gid = gid;
            this.wait = waitState;
            this.singleton = singletonHandler;
        }

        public ByteMessage handleRequest(Byte gid, ByteMessage req) {
            if(!singleton) assertTrue(gid.equals(this.gid));
            Event evt;
            
            try{
                evt = new Event(req.getMessage());
            }catch(ParserException ex){ 
                fail();
                return null;
            }
            
            ByteMessage bm = new ByteMessage(1);
            bm.setStatus(DebuggerConstants.OK);
            
            try{
                bm.writeAt(0, Byte.parseByte(evt.getAttribute("n")));
            }catch(Exception ex){
                fail();
            }
            
            if(wait != 0){
                synchronized(this){
                    try{this.wait(wait);}catch(InterruptedException ex){}
                }
            }
            
            return bm;
        }
    }
    
    private class Client implements Runnable{

        private Byte gid;
        private IGlobalAgent agent;
        private boolean singleton;
        
        public Client(Byte gid, boolean singleton){
            this.gid = gid;
            this.agent = getProxy(gid);
            this.singleton = singleton;
        }
        
        public void run() {
            Thread senders [] = new Thread[5];
            for(int i = 0; i < senders.length; i++) 
                senders[i] = new Thread(new Sender(i, agent));
            
            for(Thread t : senders) t.start();
            
            for(Thread t : senders) try{ t.join(); }catch(InterruptedException ex){fail();}
            try{
                agent.dispose();
            }catch(Exception ex){ fail(); }
            
        }
        
        private class Sender implements Runnable{
            private int what;
            private IGlobalAgent proxy;
            
            public Sender(int what, IGlobalAgent proxy){
                this.what = what;
                this.proxy = proxy;
            }
            
            public void run(){
                try{
                    Map<String, String> encoded = new HashMap<String, String>();
                    encoded.put("n", Integer.toString(what));
                    nPrints.incrementAndGet();
                    Event evt = new Event(encoded, DebuggerConstants.CLIENT_UPCALL);
                    byte res = proxy.syncNotify(evt);
                    System.out.println(res);
                    if(res != (byte)what) fail();
                }catch(Exception ex){ 
                    ex.printStackTrace();
                    fail(); 
                }
            }
        }
        
    }
    
    private IGlobalAgent getProxy(Byte gid){
        try{
            return
                new GlobalAgentProxyImpl(new PooledConnectionManager(1,
                    "localhost", PORT, new WaitUntilAvailablePolicy()), gid);
        }catch(UnknownHostException ex){
            fail();
            return null;
        }
    }

}
