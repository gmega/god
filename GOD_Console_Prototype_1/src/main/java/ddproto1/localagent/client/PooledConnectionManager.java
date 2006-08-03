/*
 * Created on Sep 21, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: PooledConnectionManager.java
 */

package ddproto1.localagent.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ddproto1.util.commons.ByteMessage;
import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.ResourceLimitReachedException;
import ddproto1.exception.commons.CommException;

/**
 * @author giuliano
 *
 */
public class PooledConnectionManager implements IConnectionManager {

    private static final Logger logger = Logger.getLogger(PooledConnectionManager.class);
    
    private static final int REAP_INTERVAL=300000;
    private static final long CONN_TIMEOUT=60000;
    private static final int VALIDATE_TIMEOUT=6000;

    private IProtocolHook iph;
    private List <ConnectionImpl> pooled;
    private List <ConnectionImpl> inuse;
    private int connMax;
    private InetAddress address;
    private int port;
    private ConnectionReaper reaper;
    
    private boolean disposed = false;
    
    private AcquisitionPolicy acquisitionPolicy;
    
    public PooledConnectionManager(int poolsize, String address, int port, AcquisitionPolicy acquisitionPolicy)
    	throws UnknownHostException
    {
        pooled = new LinkedList <ConnectionImpl> ();
        inuse = new LinkedList <ConnectionImpl> ();
        
        this.acquisitionPolicy = acquisitionPolicy;
        acquisitionPolicy.setManager(this);
        
        connMax = poolsize;
        this.port = port;
        this.address = InetAddress.getByName(address);
        
        /* Starts the connection reaper */
        reaper = new ConnectionReaper(REAP_INTERVAL);
        Thread reaperThread = new Thread(null, reaper, "Connection Reaper");
        
        /* Will die along with the user threads */
        reaperThread.setDaemon(true);
        reaperThread.start();
        
        /* Inserts the connection killer */
        Runtime.getRuntime().addShutdownHook(new Thread(new Killer()));
    }
    
    /* (non-Javadoc)
     * @see ddproto1.localagent.client.IConnectionManager#acquire()
     */
    public synchronized IConnection acquire() 
    	throws CommException, ResourceLimitReachedException
    {
        if(isDisposed()) throw new IllegalStateException();
        try{
            return acquisitionPolicy.handleAcquire();
        }catch(IOException ex){
            throw new CommException("Failed to acquire connection.", ex);
        }
    }
    
    protected void createConnection() throws IOException, CommException{
        if(logger.isDebugEnabled()) logger.debug("Created connection.");
        Socket newConn = new Socket(address, port);
        ConnectionImpl ci = new ConnectionImpl(newConn);
        iph.pre_open(ci);
        pooled.add(ci);
    }
    
    protected synchronized IConnection removeFromPool() throws ResourceLimitReachedException{
        ConnectionImpl ci = pooled.remove(0);
        inuse.add(ci);
        return ci;
    }
    
    protected synchronized void returnToPool(ConnectionImpl conn){
        if(!inuse.remove(conn))
            throw new IllegalArgumentException("Connection does not belong to this pool.");
        pooled.add(conn);
        acquisitionPolicy.handleReturn();
    }
    
    protected boolean hasPooledConnections(){
        return pooled.size() != 0;
    }
    
    protected boolean canCreateConnection(){
        if(logger.isDebugEnabled()) 
            logger.debug("Pool size: " + pooled.size() + ", In use: " + inuse.size());
        return (pooled.size() + inuse.size() < connMax); 
    }
    
    private synchronized void reapConnections(){
        if(isDisposed()) return;
        Iterator it = inuse.iterator();
        long stale = System.currentTimeMillis() - CONN_TIMEOUT;
        while(it.hasNext()){
            ConnectionImpl ci = (ConnectionImpl)it.next();
            if(stale > ci.getTimestamp() && !ci.validate()){
                try{
                    /* Connection is probably dead so don't call
                     * pre_close.
                     */
                    ci.close();
                    acquisitionPolicy.handleReturn();
                }catch(IOException e) { }
                logger
                        .debug("Invalid (possibly dead) connection discarded. Pool size is now "
                                + pooled.size() + ", was " + (pooled.size() + 1) + ".");
                pooled.remove(ci);
            }
        }
    }
    
    public synchronized void closeAll()
    	throws CommException
    {
        if(isDisposed()) throw new IllegalStateException();
        boolean doPooled = true;
        
        Iterator it;
        
        while(true){
            
            int to;
            
            if(doPooled){
                logger.info("Closing pooled connections...");
                to = pooled.size();
                it = pooled.iterator();
                doPooled = false;
            }else{
                it = inuse.iterator();
                to = inuse.size();
                if(!inuse.isEmpty())
                    logger.warn("I'll close connections marked as 'in use' for you, but " +
                    		"you should consider doing your own housekeeping in the future.");
            }
       
            int i = 0;
            
            while(it.hasNext()){
                ConnectionImpl ci = (ConnectionImpl)it.next();
                try{
                    iph.pre_close(ci);
                    ci.close();
                    it.remove();
                    i++;
                }catch(IOException e){
                    logger.error("Error - failed to close pooled connection.", e);
                }
            }
            
            logger.info("closed ["+i+"/"+to+"] connections.");
            
            if(doPooled == false) break;
        }
    }

    public synchronized void setProtocolHook(IProtocolHook iph){
        if(isDisposed()) throw new IllegalStateException();
        if(!inuse.isEmpty())
            throw new IllegalStateException("You cannot switch the protocol hook" +
            		" while there are still active connections.");
        this.iph = iph;
    }
    
    protected boolean isDisposed(){
        return disposed;
    }
    
    public void dispose() throws CommException{
        synchronized(this){
            if(isDisposed()) disposed(true);
        }
        try{
            reaper.dispose();
            closeAll();
        }catch(CommException ex){
            throw ex;
        }catch(Exception ex){
            throw new CommException("Error while disposing global agent proxy.", ex);
        }
    }
    
    protected void disposed(boolean disposed){
        this.disposed = disposed;
    }
    
    private class ConnectionImpl implements IConnection{

        private final byte [] controlmessage = { DebuggerConstants.ECHO_REQUEST };
        
        private Socket realConnection;
        private BufferedInputStream in;
        private BufferedOutputStream out;
        private long timestamp;
        
        private ConnectionImpl(Socket s)
        	throws SocketException, IOException
        {
            realConnection = s;
            realConnection.setKeepAlive(true);
            in = new BufferedInputStream(realConnection.getInputStream());
            out = new BufferedOutputStream(realConnection.getOutputStream());
        }
        
        private void close()
        	throws IOException
        {
            realConnection.close();
        }
        
        public synchronized long getTimestamp(){
            return timestamp;
        }
        
        public synchronized void setTimestamp(long timestamp){
            this.timestamp = timestamp;
        }
        
        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IConnection#send(ddproto1.util.ByteMessage)
         */
        public void send(ByteMessage bm) 
        	throws CommException
        {
            try{
                out.write(bm.getBytes());
                out.flush();
            }catch(IOException e){
                throw new CommException("Error while sending message.", e);
            }
        }

        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IConnection#recv(int)
         */
        public ByteMessage recv(int timeout) 
        	throws CommException
        {
            ByteMessage bm;
            
            try{
                realConnection.setSoTimeout(timeout);
                bm = ByteMessage.read(in);
            }catch(SocketTimeoutException e){
                throw new CommException("recv has timed out.", e);
            }catch(CommException e){
                throw e;
            }catch(Exception e){
                throw new CommException(e.toString(), e);
            }
            
            return bm; 
        }

        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IConnection#release()
         */
        public void release() {
            returnToPool(this);
        }

        /* (non-Javadoc)
         * @see ddproto1.localagent.client.IConnection#validate()
         */
        public boolean validate() {
            try{
                send(new ByteMessage(controlmessage));
                ByteMessage stats = recv(VALIDATE_TIMEOUT);
                if(stats.getStatus() != DebuggerConstants.OK) return false;
            }catch(CommException e){
                return false;
            }
            return true;
        }
    }
    
    private class ConnectionReaper implements Runnable {
        private int interval;
        private boolean active = true;

        private ConnectionReaper(int interval) {
            this.interval = interval;
            System.out.println("Reaper created");
        }

        public void run() {

            while (true) {
                try {
                    synchronized (this) {
                        if(!active) break;
                        wait(interval);
                        if(!active) break;
                    }
                } catch (InterruptedException e) {
                }
                logger
                        .debug("Connection reaper is now searching for dead connections.");
                PooledConnectionManager.this.reapConnections();
            }

        }
        
        protected void dispose(){
            synchronized(this){
                active = false;
                notify();
            }
        }
    }
    
    private class Killer implements Runnable{
        public void run(){
            try{
                PooledConnectionManager.this.closeAll();
            }catch(CommException e){
                logger.error("Error while closing connection", e);
            }
        }
    }
    
    public static abstract class AcquisitionPolicy{
        protected PooledConnectionManager pcm;
        
        protected void setManager(PooledConnectionManager parent){
            if(pcm != null) throw new IllegalStateException("Policies cannot be shared.");
            pcm = parent;
        }

        protected abstract IConnection handleAcquire()
                throws ResourceLimitReachedException, CommException, IOException;
        protected abstract void handleReturn();
    }
    
    public static class WaitUntilAvailablePolicy extends AcquisitionPolicy{
        
        @Override
        protected IConnection handleAcquire() throws ResourceLimitReachedException, CommException, IOException {
            synchronized(pcm){
                while(true){
                    if(pcm.hasPooledConnections())
                        return pcm.removeFromPool();
                    else if(pcm.canCreateConnection()){
                        pcm.createConnection();
                        return pcm.removeFromPool(); // Should work, since we're holding the lock.
                    }
                
                    try{ pcm.wait(); } catch(InterruptedException ex) { }
                }
            }
        }
        
        @Override
        protected void handleReturn() {
            synchronized(pcm){
                pcm.notify();
            }
        }
    }
    
    public static class ThrowsExceptionPolicy extends AcquisitionPolicy{

        @Override
        protected IConnection handleAcquire() throws ResourceLimitReachedException, CommException, IOException {
            synchronized (pcm) {
                if (pcm.hasPooledConnections())
                    return pcm.removeFromPool();
                else if (pcm.canCreateConnection()){
                    pcm.createConnection();
                    return pcm.removeFromPool();
                }
                else
                    throw new ResourceLimitReachedException(
                            "Cannot acquire connection. Pool depleted.");
            }
        }
        @Override
        protected void handleReturn() { }
    }
}

