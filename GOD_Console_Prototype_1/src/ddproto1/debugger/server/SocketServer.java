/*
 * Created on Sep 8, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SocketConfigurationServer.java
 */

package ddproto1.debugger.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ddproto1.util.commons.ByteMessage;
import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.CommException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.util.MessageHandler;
import ddproto1.util.Semaphore;

/**
 * Receives socket connections and dispatches them to connection handlers.
 * 
 * Version 0.1:
 *   - Thread/connection pooling (1)
 *   - Threads in the pool never die (2)
 *   - Rigid structure (3)
 *   
 * 1 is OK. 2 might be supplanted by some sort of thread expiration policy based on
 * idle time (though the load is planned to be so heavy that we'll probably hardly
 * get any idle threads). 3 only goes away with redesign. 
 * 
 * 
 * @author giuliano
 *
 */
public class SocketServer implements Runnable{
    
    private static final int LAST_EIGHT = 65280;
    private static final int FIRST_EIGHT = 255;
    
    private static final byte OPENING = 0;
    private static final byte HANDLING = 1;
    private static final byte CLOSING = 2;
    private static final byte HANDLE_SPECIAL = 3;
    
    private static final int DEFAULT_CLIENT_TIMEOUT = 0;
    
    private int port;
    private int max_threads;
    private int max_connections;
    private int thread_load = 0;
    private int idle_threads = 0;
    private List<Socket>conn_queue;
    
    private boolean active = true;
    
    private Map<Byte, IRequestHandler> handlers = new HashMap<Byte, IRequestHandler>();
    
    private Semaphore handlerSema = new Semaphore(0);
    
    private ServerSocket ssocket = null;
    private Logger logger = Logger.getLogger("agent.global");
    
    public SocketServer(int port, int max_threads, int max_connections){
        this.max_threads = max_threads;
        this.port = port;
        this.max_connections = max_connections;
        conn_queue = new LinkedList<Socket>();
    }
    
    public void registerNode(Byte gid, IRequestHandler handler)
    	throws ConfigException
    {
        /* Might seem odd, but the conn_queue synchronization avoids 
         * the situation when we modify the handler table at the same
         * time we're using it.
         */
        synchronized(conn_queue){
            if(handlers.containsKey(gid)){
                throw new ConfigException("There is already a node registered under that gid.");
            }
        
            handlers.put(gid, handler);
        }
    }
    
    public void unregisterNode(Byte gid){
        synchronized(conn_queue){
            Object obj = handlers.remove(gid);
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        
        try{
            ssocket = new ServerSocket(port);
        }catch(Exception e){
            logger.error("An error has occurred while creating the\n" +
            		" socket for the Configuration Server\n" +
            		" at port " + port + ". The server is NOT running.", e);
        }
         
        logger.info("Starting server at port [" + port
                + "] with [" + max_threads + "] maximum thread load and " +
                		"["  + max_connections + "] connection queue size.");
        
        try{
            while(active){
                Socket inbound = ssocket.accept();
                
                logger.debug("Accepted connection from " + inbound.getRemoteSocketAddress() + " at port " + inbound.getLocalPort() + ".");
                                
                synchronized(conn_queue){
                    if(conn_queue.size() == max_connections){
                        refuseConnection(inbound);
                        inbound.close();
                        return;
                    }
                    
                    conn_queue.add(0, inbound);
                    
                    /* Unblocks a thread to handle this request. */
                    /* Note that the handler thread will halt as soon
                     * as it tries to acquire the connection from 
                     * the pool, unless this synchronized block has
                     * already completed.
                     */
                    handlerSema.v();
                    handleConnection();
                }
                
            }
            
        }catch(IOException e){
            throw new NestedRuntimeException(e);
        }
    }
    
    private void handleConnection(){
        /* Creates a new thread only if there are
         * no idle threads.
         */
        if(thread_load == max_threads || idle_threads > 0){
            /* Next thread who wants to grab a connection
             * from the pool will not wait.
             */
            idle_threads--;
        }else{
            thread_load++;
            ConnectionHandler ch = new ConnectionHandler();
            Thread handlerThread = new Thread(ch);
            handlerThread.setName("Handler Thread");
            handlerThread.setDaemon(true);
            handlerThread.start();
        }
    }
    
    private void refuseConnection(Socket s)
    	throws IOException
    {
        ByteMessage outbound = new ByteMessage(0);
        outbound.setStatus(DebuggerConstants.MAX_CONNECTIONS_REACHED_ERR);
        s.getOutputStream().write(outbound.getBytes());
        s.close();
    }
    
    private class ConnectionHandler implements Runnable{

        private ConnectionHandler(){ }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {

            /*
             * REMARK No need for thread_load-- on try-finally block since all thread
             * paths are covered. 
             */
            while (true) {

                Socket destiny;

                BufferedOutputStream out = null;
                BufferedInputStream in = null;

                destiny = acquireFromPool();

                /* Should -never- happen */
                assert (destiny != null);

                try {
                    
                    destiny.setKeepAlive(true);
                    destiny.setSoTimeout(DEFAULT_CLIENT_TIMEOUT);

                    out = new BufferedOutputStream(destiny.getOutputStream());
                    in = new BufferedInputStream(destiny.getInputStream());

                    processRequests(in, out);
                    
                                        
                } catch(RuntimeException e){
                    logger.error(e.toString(), e);
                } catch (Exception e) {
                    if(e.getMessage().startsWith("Broken pipe")){
                        MessageHandler mh = MessageHandler.getInstance();
                        mh.getWarningOutput().println("Warning - connection from " + destiny.getInetAddress() + " died without closing.");
                        /* FIXED: a 'return' here caused the thread to die at the 
                         * same time it incremented the idle_threads counter. 
                         * This made the connection dispatch thread think there
                         * were a handler thread available when there were actually
                         * none, causing things to grind into a halt.
                         */
                        idle_threads++;
                        continue;
                    }
                    
                    logger
                            .error("Error while servicing request from client "
                                    + destiny.getInetAddress()
                                            .getCanonicalHostName(), e);
                    
                } finally {
                    if(destiny != null){
                        try{
                            destiny.close();
                        }catch(Throwable t){ 
                            logger.error("Could not close connection.", t);
                        }
                    }
                }
                
                /* Unsynchronized increment is not really a problem
                 * since the worse that could happen is a new thread
                 * being created when there was already one avaliable.
                 * 
                 * FIX: This increment was inside the finally block and that is incorrect.
                 * The idle_threads counter should not be incremented if the thread 
                 * abnormally dies.
                 */
                idle_threads++; 
            }
        }
        
        private Socket acquireFromPool(){
            // Guaranteed to have a connection waiting.
            handlerSema.p();
                        
            synchronized(conn_queue){
                return (Socket)conn_queue.remove(conn_queue.size()-1);
            }
        }
        
        private void processRequests(BufferedInputStream in, BufferedOutputStream out)
        	throws IOException, SocketTimeoutException, CommException
        {
            byte state = OPENING;
            byte backstate;
            
            ByteMessage inbound = null;
            ByteMessage outbound = null;
            Byte gid = null;
            
            boolean connected = true;
            
            IRequestHandler handler = null;
            
            /* 
             * The protocol goes like this:
             * 
             * message(1) - [DebuggerConstants.START_REQUEST][GID]
             * message(2) - [DebuggerConstants.REQUEST][24-bit bytesize][request bytes]
             * message(3) - [DebuggerConstants.REQUEST][24-bit bytesize][request bytes]
             *   ...
             * message(n) - [DebuggerConstants.END_REQUEST]
             * 
             * Responses are:
             * reply(1) - [STATS]
             * reply(2) - [STATS][24-bit bytesize][reply bytes]
             * reply(3) - [STATS][24-bit bytesize][reply bytes]
             *  ...
             * reply(n) - [STATS]
             * 
             * Where [STATS] can be XXX_ERR (see DebuggerConstants) or OK. 
             */
            try {
                while (connected) {

                    if (state != CLOSING)
                        inbound = ByteMessage.read(in);

                    if ((outbound = handleSpecial(inbound)) != null) {
                        backstate = state;
                        state = HANDLE_SPECIAL;
                    }

                    switch (state) {
                    
                    case OPENING: 
                        outbound = new ByteMessage(0);

                        /*
                         * Looks at the initial message format status should be
                         * START_REQUEST first body byte should contain the
                         * caller GID.
                         */
                        if ((inbound.getStatus() != DebuggerConstants.START_REQUEST)
                                || inbound.getSize() != 1) {
                            outbound.setStatus(DebuggerConstants.PROTOCOL_ERR);
                            connected = false;
                            break;
                        }

                        /*
                         * If the header is correct, checks to see if there is a
                         * handler registered for this node.
                         */
                        gid = new Byte(inbound.get(0));
                        if (!handlers.containsKey(gid)) {
                            logger
                                    .warn("No handler registered for node with gid "
                                            + gid);
                            outbound.setStatus(DebuggerConstants.PROTOCOL_ERR);
                            break;
                        }

                        /* If everything is OK, reports it back to the caller */
                        outbound.setStatus(DebuggerConstants.OK);
                        handler = (IRequestHandler) handlers.get(gid);
                        state = HANDLING;

                        break;
                    

                    case HANDLING: 
                        /* Valid requests for this state are: */

                        /* Either the client wants to end this connection */
                        if (inbound.getStatus() == DebuggerConstants.END_REQUEST) {
                            outbound = new ByteMessage(0);
                            outbound.setStatus(DebuggerConstants.OK);
                            state = CLOSING;
                        }
                        /*
                         * Or he wants to make a request/notification to the
                         * registered handler
                         */
                        else if (inbound.getStatus() == DebuggerConstants.REQUEST
                                || inbound.getStatus() == DebuggerConstants.NOTIFICATION) {
                            outbound = handler.handleRequest(gid, inbound);
                        }
                        /* Or it's an echo request */
                        else if (inbound.getSize() == DebuggerConstants.ECHO_REQUEST) {
                            outbound = new ByteMessage(0);
                            outbound.setStatus(DebuggerConstants.OK);
                        }
                        /* Or the message is messed up */
                        else {
                            outbound = new ByteMessage(0);
                            outbound.setStatus(DebuggerConstants.PROTOCOL_ERR);
                        }
                        break;
                    
                    
                    case HANDLE_SPECIAL: 
                        break;
                    

                    case CLOSING: 
                        return;
                        
                    
                    default:	
                        throw new RuntimeException("Debugger event handler - unknown state.");
                    
                    }

                    if (outbound != null) {
                        out.write(outbound.getBytes());
                        out.flush();
                    }
                }
                
            } 
            /* This catch block implements a sort of an exception handling advice.
             * It detects the exception and tells the client that something went wrong.
             * I then rethrows the exception to the event dispatcher.
             */
            catch (RuntimeException e) {
                outbound = new ByteMessage(0);
                /* REMARK We perhaps could insert the serialized exception into the message. */ 
                outbound.setStatus(DebuggerConstants.HANDLER_FAILURE_ERR);
                out.write(outbound.getBytes());
                out.flush();
                
                /* Rethrows the exception */
                throw e;
            }

        }
        
        private ByteMessage handleSpecial(ByteMessage msg){
            if(msg.getStatus() == DebuggerConstants.ECHO_REQUEST){
                ByteMessage pong = new ByteMessage(0);
                pong.setStatus(DebuggerConstants.OK);
                return pong;
            }
                
            return null;
        }
    }
}
