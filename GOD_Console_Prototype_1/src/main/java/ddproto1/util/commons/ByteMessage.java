/*
 * Created on Sep 13, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ByteArray.java
 */

package ddproto1.util.commons;

import java.io.BufferedInputStream;
import java.io.IOException;

import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.commons.CommException;


/**
 * @author giuliano
 *
 */
public class ByteMessage {
    
    private static final int base = DebuggerConstants.SIZE_BYTES + 1;
    
    private byte [] msg;
    private byte [] body;
    
    private static final int NSIGMASK = 127;
    private static final int SIGMASK = 128;
    

    public ByteMessage(int size){
        if(size < 0)
            throw new IllegalArgumentException("Size must be a positive integer (got " + size + ")");
        
        msg = new byte[size + base];
        body = new byte[size];
        msg[0] = -1;
        int mask = 255;
        for(int i = 0; i < DebuggerConstants.SIZE_BYTES; i++){
            msg[1+i] = (byte)(size & mask);
            size >>= 8;
        }
    }
    
    public ByteMessage(byte [] body){
        this(body.length);
        System.arraycopy(body, 0, this.body, 0, body.length);
        System.arraycopy(body, 0, msg, base, body.length);        
    }
    
    public ByteMessage(int size, ByteMessage old){
        this(size);
        System.arraycopy(old.msg, 0, msg, base, old.msg.length);
    }
    
    public byte[] getBytes(){
        return (byte[])msg.clone();
    }
    
    public byte[] getMessage(){
        return body;
    }
    
    public void writeAt(int i, byte val){
        body[i] = msg[base+i] = val;
    }
    
    public void writeAt(int i, byte [] val){
        if(val.length + i > body.length)
            throw new ArrayIndexOutOfBoundsException(i + val.length);
        
        System.arraycopy(val, 0, body, i, val.length);
        System.arraycopy(val, 0, msg, i + base, val.length);
    }
    
    public byte get(int i){
        assert(body[i] == msg[base+i]);
        return msg[base+i];
    }
    
    public byte getStatus(){
        return msg[0];
    }
    
    public void setStatus(byte b){
        msg[0] = b;
    }

    public int getSize(){
        return readSize(msg);
    }
    
    public static ByteMessage read(BufferedInputStream byteStream)
    	throws IOException, CommException
    {
        byte [] header = new byte[base];
        int size = 0;
       
        // Reads the message header
        byteStream.read(header);
        
        size = readSize(header);
        
        ByteMessage bm = new ByteMessage(size);
        bm.setStatus(header[0]);
        
        /* Since our stream is buffered we can read it like this without
         * being too inefficient.
         */
        for(int i = 0; i < size; i++){
            /* FIXME Corrupted headers may cause this thread to block undefinitely.
             * Don't know how to fix it since I can't do something like:
             * 
             * if(byteStream.available() != size) throw ...
             * 
             * Because I can't know in advance if the message has the incorrect size or if 
             * it's just the rest of it that hasn't arrived yet.
             */
            byte r = (byte)byteStream.read();
            if(r == -1)
                throw new CommException("Unexpected end-of-stream reached - message header might be corrupted.");
            bm.writeAt(i, r);
        }
        
        return bm;
    }
    
    private static int readSize(byte [] header){
        
        int size = 0;
        int aux;
        
        for(int i = DebuggerConstants.SIZE_BYTES; i > 0; i--){
            size <<= 8;
            aux = header[i];
            
            if(aux >= 0)
                size = (char)(size | header[i]);
            else{
                // Removes the signal bit.
                aux &= NSIGMASK;
                // ORs the signal-bit removed data in.
                size |= aux;
                /* ORs the signal-bit back in, but this time it won't be
                 * regarded as a signal bit. 
                 */ 
                size |= SIGMASK;
            }
        }
        
        return size;
    }
}
