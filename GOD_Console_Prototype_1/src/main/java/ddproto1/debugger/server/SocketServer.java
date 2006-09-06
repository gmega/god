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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ddproto1.util.collection.UnorderedMultiMap;
import ddproto1.util.commons.ByteMessage;
import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.server.exception.ActivationFailedException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.CommException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.util.IServiceLifecycle;
import ddproto1.util.MessageHandler;
import ddproto1.util.Semaphore;

/**
 * Receives socket connections and dispatches them to connection handlers.
 * @note The current implementation is completely flawed. It probably
 * requires a complete rewrite.
 * 
 * Version 0.1:
 *   - Thread/connection pooling (1)
 *   - Threads in the pool never die (2)
 *   - Rigid, poorly modularized structure (3)
 *   - Thread-per-connection policy (4)
 *   - Synchronization policy is flawed because I didn't understand
 *     the JMM when I first wrote this (5).
 *   
 * 1 is OK. 2 might be supplanted by some sort of thread expiration policy based on
 * idle time (though the load is planned to be so heavy that we'll probably hardly
 * get any idle threads). 3 only goes away with redesign. 4 is pretty bad. 5 is 
 * more than bad, it's terrible.   
 * 
 * 
 * @author giuliano
 *
 */
public class SocketServer implements Runnable, IServiceLifecycle{
    
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
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(SocketServer.class);
   
    private boolean activating = false;
    private boolean deactivating = false;
    private boolean active = false;
    
    private Object stateLock = new Object();
    
    private Map<Byte, IRequestHandler> handlers = new HashMap<Byte, IRequestHandler>();
    private List<ConnectionHandler> activeHandlers = new ArrayList<ConnectionHandler>();
    private UnorderedMultiMap<IRequestHandler, ConnectionHandler> boundHandlers = 
        new UnorderedMultiMap<IRequestHandler, ConnectionHandler>(HashSet.class);
    
    private Semaphore handlerSema = new Semaphore(0);
    private ServerSocket ssocket = null;
    
    private IRequestHandler singletonHandler = null;
    
    public SocketServer(){
        conn_queue = new LinkedList<Socket>();
    }
    
    public void setPort(int port){
        checkDeactivated();
        this.port = port;
    }
    
    public void setMaxThreads(int max_threads){
        checkDeactivated();
        this.max_threads = max_threads;
    }
    
    public void setMaxConnections(int max_connections){
        checkDeactivated();
        this.max_connections = max_connections;
    }
    
    private synchronized void checkDeactivated(){
        if(!canActivate())
            throw new IllegalStateException("Must stop server before changing parameters");
    }
    
    public void registerHandlerForNode(Byte gid, IRequestHandler handler)
    	throws ConfigException
    {
        /* Might seem odd, but the conn_queue synchronization avoids 
         * the situation when we modify the handler table at the same
         * time we're using it.
         */
        synchronized(conn_queue){
            if(handlers.containsKey(gid)){
                throw new ConfigException("There is already a handler registered for this node.");
            }
        
            handlers.put(gid, handler);
        }
    }
    
    public boolean unregisterHandlerForNode(Byte gid){
        synchronized(conn_queue){
            IRequestHandler handler = handlers.get(gid);
            if(handler == null) return false; // No handler registered for this gid.
            
            if(hasOngoingRequests(handler))
                throw new IllegalStateException("There are open connections originating from " +
                        "this node. You must shut it down first.");
            
            handlers.remove(gid);
                       
            return true;
        }
    }
    
    public void setSingletonHandler(IRequestHandler handler)
        throws ConfigException
    {
        if(singletonHandler != null)
            throw new IllegalStateException("There's already a singleton handler registered. " +
                    "You must clear it first.");
        singletonHandler = handler;
    }
            
    public void clearSingletonHandler()
        throws ConfigException
    {
        if(singletonHandler == null)
            throw new ConfigException("You cannot clear the singleton handler when there's none.");
        
        if(hasOngoingRequests(singletonHandler))
            throw new IllegalStateException("There are one or more connections bound to the singleton handler.");
        
        synchronized(conn_queue){
            singletonHandler = null;
        }
    }
    
    public boolean hasSingletonHandler(){
    		return singletonHandler != null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        
        try{
            /** We synchronize because:
             * 1) stop must close the ssocket.
             * 2) if ssocket is null, then stop won't close it (it will just return).
             * 3) if stop is issued, then the server must stop.
             * 
             * It could be the case that 'stop' is issued just before the socket is
             * created. If that's the case, this thread might get to ssocket.accept() and
             * the server will wait for connections even though it's supposed to be stopped.
             *  
             */
            synchronized(conn_queue){
                if(!isDeactivating()){
                    ssocket = new ServerSocket(port);
        				activated();
                } else { return; } 
            }
        }catch(Exception e){
            logger.error("An error has occurred while creating the\n" +
            		" socket for the Configuration Server\n" +
            		" at port " + port + ". The server is NOT running.", e);
            deactivated();
            return;
        }
         
        logger.info("Starting server at port [" + port + "] with ["
                + max_threads + "] maximum thread load and " + "["
                + max_connections + "] connection queue size.");

        while (isReady()) {
            Socket inbound;

            try {
                inbound = ssocket.accept();
            } catch (IOException e) {
                if(!isDeactivating() && !isDeactivated())
                    logger.error("Error while accepting connection.", e);
                continue;
            }

            logger.debug("Accepted connection from "
                    + inbound.getRemoteSocketAddress() + " at port "
                    + inbound.getLocalPort() + ".");

            synchronized (conn_queue) {
                if (conn_queue.size() == max_connections || !isReady()) {
                    refuseConnection(inbound);
                    return;
                }

                conn_queue.add(0, inbound);

                /* Unblocks a thread to handle this request. */
                /*
                 * Note that the handler thread will halt as soon as it tries to
                 * acquire the connection from the pool, unless this
                 * synchronized block has already completed.
                 */
                handlerSema.v();
                handleConnection();
            }

        }

    }
    
    private void handleConnection(){
        /*
         * Creates a new thread only if there are no idle threads.
         */
        if(idle_threads > 0){
            /*
             * Next thread who wants to grab a connection from the pool will not
             * wait.
             */
            idle_threads--;
        }else if(thread_load <= max_threads){
            thread_load++;
            if(logger.isDebugEnabled())logger.debug("Thread load is " + thread_load);
            ConnectionHandler ch = new ConnectionHandler("Non-JDI Message Handler - " + thread_load);
            this.handlerActivated(ch);
            ch.go();
        }
        
        /* No idle threads and thread load is maximum. The only
         * option for this request is for it to remain queued 
         * until there's an idle handler available.
         */
    }
    
    protected void handlerActivated(ConnectionHandler cg){
        synchronized(conn_queue){
            activeHandlers.add(cg);
        }
    }
    
    protected void handlerDeactivated(ConnectionHandler cg){
        synchronized(conn_queue){
            activeHandlers.remove(cg);
        }
    }
    
    protected void bindHandler(IRequestHandler handler, ConnectionHandler ch){
        synchronized(boundHandlers){
            boundHandlers.add(handler, ch);
        }
    }
    
    protected void unbindHandler(IRequestHandler handler, ConnectionHandler ch){
        synchronized(boundHandlers){
            boundHandlers.remove(handler, ch);
            if(boundHandlers.size(handler) == 0)
                boundHandlers.removeClass(handler);
        }
    }
    
    protected boolean hasOngoingRequests(IRequestHandler handler){
        synchronized(boundHandlers){
            return boundHandlers.size(handler) != 0;
        }
    }
    
    protected List<ConnectionHandler> activeHandlers(){
        return (List)((ArrayList)activeHandlers).clone();
    }
    
    private void refuseConnection(Socket s)
    {
        try{
            ByteMessage outbound = new ByteMessage(0);
            outbound.setStatus(DebuggerConstants.MAX_CONNECTIONS_REACHED_ERR);
            s.getOutputStream().write(outbound.getBytes());
            s.close();
            logger.error("Global agent had to refuse a connection. Connection load " +
                    "is too high for current parameters. ");
        }catch(IOException ex){
            logger.error("Error while refusing connection.", ex);
        }
    }
    
    protected IRequestHandler getHandler(Byte gid){
        synchronized (conn_queue) {
            if (singletonHandler == null)
                return handlers.get(gid);
            return singletonHandler;
        }
    }
    
    private class ConnectionHandler implements Runnable{
        
        Socket current;
        
        private boolean running = true;
        private String label;

        private ConnectionHandler(String label){ 
            this.label = label;            
        }
        
        public void shutdown(){
            try{
                synchronized(this){
                    if(!isRunning()) return;
                    running = false;
                }
                current.close();
            }catch(IOException ex){
                logger.error("Error while closing connection.", ex);
            }
        }
        
        public boolean isRunning(){
            return running;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {

            /*
             * REMARK No need for thread_load-- on try-finally block since all thread
             * paths are covered. 
             */
            while (true) {

                BufferedOutputStream out = null;
                BufferedInputStream in = null;

                current = acquireFromPool();

                /* Should -never- happen */
                assert (current != null);

                try {
                    
                    current.setKeepAlive(true);
                    current.setSoTimeout(DEFAULT_CLIENT_TIMEOUT);

                    out = new BufferedOutputStream(current.getOutputStream());
                    in = new BufferedInputStream(current.getInputStream());

                    processRequests(in, out);
                    
                                        
                } catch(RuntimeException e){ // Cuts off runtime excepitons.
                    logger.error(e.toString(), e);
                } catch (Throwable e) {
                    if(e instanceof IOException){ // Exceptions, errors, etc.
                        
                        /** We're shutting down, so this is not an error. */
                        if(!running){
                            thread_load--;
                            return;
                        }
                                                       
                        if(e.getMessage().startsWith("Broken pipe")){
                            logger.warn("Warning - connection from " + current.getInetAddress() + " died without closing.");
                        }else{
                            logger
                                .error("Error while servicing request from client "
                                        + current.getInetAddress()
                                            .getCanonicalHostName(), e);
                        }
                    }
                    
                } finally {
                    if(current != null){
                        try{
                            current.close();
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
            byte previous, state;
            previous = state = OPENING;
            
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
                        previous = state;
                        state = HANDLE_SPECIAL;
                    }

                    switch (state) {
                    
                    /**
                     * This is the first state.
                     */
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

                        synchronized(conn_queue){
                            handler = getHandler(gid);
                            if (handler == null) {
                                logger
                                        .error("No handler registered for node with gid "
                                                + gid);
                                outbound.setStatus(DebuggerConstants.PROTOCOL_ERR);
                                break;
                            }
                            bindHandler(handler, this);
                        }
                        
                        /* If everything is OK, reports it back to the caller */
                        outbound.setStatus(DebuggerConstants.OK);
                        
                        state = HANDLING;

                        break;
                    

                    /**
                     * This state comes after OPENING. It might transition to 
                     * CLOSING or HANDLE_SPECIAL.
                     */
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
                        /* Or the message is messed up */
                        else {
                            outbound = new ByteMessage(0);
                            outbound.setStatus(DebuggerConstants.PROTOCOL_ERR);
                        }
                        break;
                    
                    
                    case HANDLE_SPECIAL:
                        state = previous;
                        break;
                    

                    case CLOSING: 
                        connected = false;
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
            }finally{
                unbindHandler(handler, this);
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
        
        public void go(){
            Thread runner = new Thread(this);
            runner.setDaemon(true);
            runner.setName(label);
            runner.start();
        }
     }
    
    public void start() 
    		throws Exception
    {
        synchronized(this){
            if(!canActivate()) throw new IllegalStateException("Cannot activate while in current state.");
            activating();
        }
        
        Thread serverThread = new Thread(this);
        serverThread.setName("DDWP Connection dispatcher");
        
        serverThread.start();
        
        synchronized(stateLock){
        		while(this.isActivating()){
        			try { stateLock.wait(); } catch(InterruptedException ex) { }
        		}
        		
        		if(!this.isActivated()) throw new ActivationFailedException();
        }
    }
    
    public void stop() {
        /** We synchronize on the conn_queue to prevent
         * new ConnectionHandlers from being activated.
         * 
         * This flag being set to false also means that 
         * connections can no longer be enqueued.
         */
        synchronized (conn_queue) {
            if(!isReady()) return;
            deactivating();
        }
        
        /** We know that all active connection handlers
         * are registered because we synchronized on the 
         * conn_queue lock.
         */
        
        try {
            /** Stop allowing new connections. */
        		synchronized(conn_queue){
        			if(ssocket != null) ssocket.close();
        		}
        } catch (IOException ex) {
            logger.error("Error while closing server-side socket.", ex);
        }
    
        /** Now we deactivate all active handlers. */
        for (ConnectionHandler ch : activeHandlers()) {
            ch.shutdown();
            handlerDeactivated(ch);
        }
        
        /** And flush the connection queue */
        for(Socket queuedSocket : conn_queue){
            try{
                queuedSocket.close();
            }catch(IOException ex){
                logger.error("Error while closing connection from client at "
                        + queuedSocket.getRemoteSocketAddress());
            }
        }
        
        conn_queue.clear();
        
        deactivated();
    }

    private boolean canActivate(){
        synchronized(stateLock){
            return !(isActivated() || isActivating() || isDeactivating());
        }
    }

    private boolean isReady(){
        synchronized(stateLock){
            return isActivated() && !isDeactivating();
        }
    }
    
    private void deactivating(){
    	/** Asserts that the transition is valid. */
    	if(!isActivated()){
    		logger.error("Illegal transition to DEACTIVATING caught.");
    		dumpFlags();
    	}
        synchronized(stateLock){
            deactivating = true;
            active = false;
        }
    }
    
    private void deactivated(){
    		if(!isActivating() && !isDeactivating()){
    			logger.error("Illegal transition to DEACTIVATED detected.");
    			dumpFlags();
    		}
    	
    		synchronized(stateLock){
    			deactivating = false;
    			activating = false;
    			stateLock.notifyAll();
    		}
    }
    
    private void activated(){
    		if(!isActivating()){
    			logger.error("Illegal transition to ACTIVATED detected.");
    			dumpFlags();
    		}
    		
    		synchronized(stateLock){
    			active = true;
    			activating = false;
    			stateLock.notifyAll();
    		}
    }
    
    private void activating(){
    		if(!isDeactivated()){
    			logger.error("Illegal transition to ACTIVATING detected.");
    			dumpFlags();
    		}
    		
    		synchronized(stateLock){
    			activating = true;
    			stateLock.notifyAll();
    		}
    }
    
    private void dumpFlags(){
    		StringBuffer flagStatus = new StringBuffer();
    		flagStatus.append(" --------- Server flag status --------- ");
    		flagStatus.append(" activating: " + this.onOrOff(isActivating()));
    		flagStatus.append(" deactivating: " + this.onOrOff(isActivating()));
    		flagStatus.append(" activated: " + this.onOrOff(isActivating()));
    		flagStatus.append(" deactivated: " + this.onOrOff(isDeactivated()));
    		logger.info(flagStatus.toString());
    }
    
    private String onOrOff(boolean flag) {
    	return (flag == true)?"ON":"OFF";
    }
    
    private boolean isActivating(){
        synchronized(stateLock){
            return activating;
        }
    }
    
    private synchronized boolean isDeactivating(){
        synchronized(stateLock){
            return deactivating;
        }
    }
    
    private synchronized boolean isActivated(){
        synchronized(stateLock){
            return active;
        }   
    }
    
    private boolean isDeactivated(){
        synchronized(stateLock){
            return(!isActivating() && !isDeactivating() && !isActivated());
        }
    }

    public int currentState() {
        if(isActivated()) return STARTED;
        else if(isActivating()) return STARTING;
        else if(isDeactivated()) return STOPPED;
        else if(isDeactivating()) return STOPPING;
        else throw new InternalError();
    }
}
