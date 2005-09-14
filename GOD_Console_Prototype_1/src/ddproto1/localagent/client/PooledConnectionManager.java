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
import ddproto1.exception.commons.CommException;

/**
 * @author giuliano
 *
 */
public class PooledConnectionManager implements IConnectionManager {
    
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
    private Logger logger;
    
    public PooledConnectionManager(int poolsize, String address, int port, Logger logger)
    	throws UnknownHostException
    {
        pooled = new LinkedList <ConnectionImpl> ();
        inuse = new LinkedList <ConnectionImpl> ();
        
        connMax = poolsize;
        this.port = port;
        this.address = InetAddress.getByName(address);
        this.logger = logger;
        
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
    	throws CommException
    {
        if((pooled.size() + inuse.size()) == connMax)
            throw new CommException("Cannot open anymore connections - pool capacity exceeded.");
        
        ConnectionImpl ci = null;
        Socket newConn = null;
        
        try {
            if (pooled.size() != 0){
                ci = (ConnectionImpl) pooled.remove(0);
                inuse.add(ci);
            } else {
                newConn = new Socket(address, port);
                ci = new ConnectionImpl(newConn);
                iph.pre_open(ci);
                inuse.add(ci);
            }
        } catch (IOException e) {
            throw new CommException("Error while creating new connection.", e);
        }
        
        return ci;
    }
    
    private synchronized void reapConnections(){
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
        if(!inuse.isEmpty())
            throw new IllegalStateException("You cannot switch the protocol hook" +
            		" while there are still active connections.");
        this.iph = iph;
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
            synchronized(PooledConnectionManager.this){            
            	inuse.remove(this);
            	pooled.add(this);            
            }
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

        private ConnectionReaper(int interval) {
            this.interval = interval;
            System.out.println("Reaper created");
        }

        public void run() {

            while (true) {
                try {
                    synchronized (this) {
                        wait(interval);
                    }
                } catch (InterruptedException e) {
                }
                logger
                        .debug("Connection reaper is now searching for dead connections.");
                PooledConnectionManager.this.reapConnections();
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
}

