/*
 * Created on Jun 17, 2006
 * 
 * file: RemoteProcessImpl.java
 */
package ddproto1.controller.remote.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import Ice.Current;
import Ice.LocalException;
import Ice.ObjectPrx;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDERR;
import ddproto1.controller.client.AMI_ControlClient_receiveStringFromSTDOUT;
import ddproto1.controller.client.ControlClientPrx;
import ddproto1.controller.remote.ServerRequestException;
import ddproto1.controller.remote._RemoteProcessDisp;

/**
 *
 * This class represents a remote process. It implements the logic for managing the streams
 * and for killing the underlying process.
 * 
 * @author giuliano
 *
 */
public class RemoteProcessImpl extends _RemoteProcessDisp implements IErrorCodes, IRemoteAccessible {
    
    private static final Logger logger = Logger.getLogger(RemoteProcessImpl.class);
    private static final int BACKOFF = 100;
    
    private Process underlying;
    private Writer stdin;
    private StreamGobbler stdout;
    private Thread stdout_thread;
    private StreamGobbler stderr;
    private Thread stderr_thread;
    private ControlClientPrx listener;
    private ProcessServerImpl parent;
    private ObjectPrx proxy;
    
    private volatile int pHandle;
    private volatile int timeout;
    private volatile int bufferlength;
    
    public RemoteProcessImpl(Process underlying, int bufferlength, int timeout, final int pHandle,
            ControlClientPrx listener, ProcessServerImpl parent){
        
        this.pHandle = pHandle;
        
        if(timeout == 0)
            logger.warn("Warning - timeout set to zero means infinite wait.");
        
        if(timeout < 0 || bufferlength <= 0)
            throw new IllegalArgumentException("bufferlength has to be positive, timeout has to be" +
                    " non-negative.");
        
        this.timeout = timeout;
        this.bufferlength = bufferlength;

        synchronized(this){
            this.underlying = underlying;
            this.stdin = new OutputStreamWriter(underlying.getOutputStream());
            this.listener = listener;
            this.parent = parent;
            initStreamGobblers();
        }
    }
    
    public synchronized ObjectPrx activateAndGetProxy(){
        if(proxy == null){
            proxy = parent.getAdapter().addWithUUID(this);
        }
        
        return proxy;
    }
    
    protected void initStreamGobblers(){
        
        final AMI_ControlClient_receiveStringFromSTDOUT cBackstdout = 
            new AMI_ControlClient_receiveStringFromSTDOUT(){
                @Override
                public void ice_response() { }
                @Override
                public void ice_exception(LocalException ex) {
                    logger.error("Error while sending STDOUT data to control client (" + pHandle + ")");
                }
        };
        
        final AMI_ControlClient_receiveStringFromSTDERR cBackstderr = 
            new AMI_ControlClient_receiveStringFromSTDERR(){
                @Override    
                public void ice_response() { }
                @Override
                public void ice_exception(LocalException ex) {
                    logger.error("Error while sending STDERR data to control client (" + pHandle + ")");
                }
        };
        
        stdout = new StreamGobbler(underlying.getInputStream(),
                new NotificationWrapper(){
                    public void doNotify(String data) {
                        getControlClient().receiveStringFromSTDOUT_async(cBackstdout, pHandle, data);
                    }
        }, true);
        
        stderr = new StreamGobbler(underlying.getErrorStream(),
                new NotificationWrapper(){
                    public void doNotify(String data) {
                        getControlClient().receiveStringFromSTDERR_async(cBackstderr, pHandle, data);
                    }
        }, true);
    }
    
    protected synchronized Process getProcess(){
        return underlying;
    }

    public boolean isAlive(Current __current) {
        try{
            getProcess().exitValue();
            return false;
        }catch (IllegalThreadStateException ex){
            return true;
        }
    }

    public synchronized void writeToSTDIN(String message, Current __current) 
        throws ServerRequestException {
        try{
            stdin.write(message.toCharArray());
            stdin.flush();
        }catch(IOException ex){
            throw new ServerRequestException("Error while writting to STDIN " + ex.getMessage(), IO_FAILURE);
        }
    }

    public int getHandle(Current __current) {
        return pHandle;
    }

    public void dispose(Current __current) {
        getProcess().destroy();
        synchronized(this){
            parent.getAdapter().remove(proxy.ice_getIdentity());
        }
        parent.disposeCalled(this);
    }
    
    protected ControlClientPrx getControlClient(){
        return listener;
    }
    
    public synchronized void beginDispatchingStreams(){
        stdout_thread = new Thread(stdout);
        stderr_thread = new Thread(stderr);
        
        stdout_thread.start();
        stderr_thread.start();
    }

    private class StreamGobbler implements Runnable{
        
        private BufferedReader reader;
        private NotificationWrapper wrapper;
        private volatile boolean closeOnExit;
        
        public StreamGobbler(InputStream is, NotificationWrapper wrapper, boolean closeOnExit){
            synchronized(this){
                reader = new BufferedReader(new InputStreamReader(is));
                this.wrapper = wrapper;
            }
            this.closeOnExit = closeOnExit;
        }
        
        protected synchronized BufferedReader getReader(){
            return reader;
        }
        
        protected synchronized NotificationWrapper getWrapper(){
            return wrapper;
        }

        public void run() {
            BufferedReader lReader = getReader();
            char buf [] = new char[bufferlength];
            
            while(true){
                try{
                    getWrapper().doNotify(tryRead(buf, lReader));     
                }catch(InterruptedException ex){
                    break;
                }catch(IOException ex){
                    /** If the process isn't alive anymore, then it's okay to have 
                     * an IOException here. */
                    if(!isAlive() || ex.getMessage().startsWith("Stream closed"))
                        break;
                    
                    logger.error("Error while processing streams for (" + pHandle + ")", ex);
                    break;
                    
                }finally{
                    try{
                        if(closeOnExit)
                            lReader.close();
                    }catch(IOException ex){
                        logger.error("Failed to close output stream.", ex);
                    }
                }
            }
        }
        
        private String tryRead(char [] buffer, BufferedReader reader)
            throws IOException, InterruptedException
        {
            long startTime = System.currentTimeMillis();
            int offset = 0; // First unused position
           
            while(true){
                int readValue = reader.read(buffer, offset, buffer.length - offset);
                
                /** Stream ended. */
                if(readValue == -1){
                    break;
                }
                
                offset += readValue;

                /** Buffer is full. We're done. */
                if(offset == buffer.length)
                    break;
                
                /** Stall. */
                Thread.sleep(BACKOFF);
                
                /** See if we timeouted. Only breaks if something has actually been
                 * read. */
                if((System.currentTimeMillis() - startTime >= timeout) && offset > 0)
                    break;
            }
            
            return new String(buffer, 0, offset);
        }
    }
    
    private interface NotificationWrapper{
        public void doNotify(String data);
    }
     
}
