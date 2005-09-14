/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: StringHandler.java
 */

package ddproto1.util.traits.commons;

import java.util.HashSet;
import java.util.Set;

import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.commons.MalformedInputException;



/**
 * @author giuliano
 *
 */
public class ConversionTrait {
    private static ConversionTrait instance;
    private static long NSIGMASK = 2147483647;
    /* Eclipse complains if I just state 2147483648 */
    private static long SIGMASK =  2147483647 + 1;
    
    private static int size = DebuggerConstants.GID_BYTES + DebuggerConstants.THREAD_LUID_BYTES;
    
    private ConversionTrait() { };
    
    public synchronized static ConversionTrait getInstance(){
        if(instance == null)
            instance = new ConversionTrait();
        
        return instance;
    }
    
    public String long2Hex(long num){
        return toHex(num);
    }
    
    public long hex2Long(String num){
        return fromHex(num);
    }
    
    public String int2Hex(int uuid){
        long res = 0;
        /* Signal trick to cast to long. */
        // Removes signal bit 
        if(uuid < 0){
            uuid &= NSIGMASK;
            res  |= uuid;
            res  |= SIGMASK;
        }else{
            res |= uuid;
        }
        
        return toHex(res);
        
    }
    
    public int hex2Int(String hex){
        long res = fromHex(hex);
        int  val = 0;
        if((res & SIGMASK) != 0){
            res &= NSIGMASK;
            val |= res;
            assert(val >= 0);
            val |= SIGMASK;
        }else
            val = (int)res;
        
        return val;
    }
    
    private String toHex(long num){
        StringBuffer hex_rep = new StringBuffer();
        
        if(num == 0) return("0x0");
        
    	do {
    	    long d = num & 0xf;
    	    hex_rep.append((char)((d < 10) ? ('0' + d) : ('a' + d - 10)));
    	} while ((num >>>= 4) > 0);
        
        hex_rep.reverse();
        
        return "0x" + hex_rep.toString();
    }
    
    private long fromHex(String num) {
        long pow = 1, val = 0;

        int i = num.indexOf('x');
        if (i == -1)
            throw new NumberFormatException();

        i++;

        for (; i < num.length(); i++) {
            char c = num.charAt(i);
            if (c >= '0' && c <= '9')
                val += (num.charAt(i) - '0');
            else if (c >= 'a' && c <= 'h')
                val += 10 + (num.charAt(i) - 'a');
            // REMARK This might be a buggy way of doing things.
            if (i < (num.length() - 1))
                val <<= 4;
        }

        return val;

    }
    
    public int toUUID(byte [] array){
        int uuid = 0;
        
        for(int i =  - 1; i >= 0; i--){
            uuid <<= 8;
            uuid |= array[i];
        }
        
        return uuid;
    }
    
    public byte [] fromUUID(int uuid){
        byte [] array = new byte[DebuggerConstants.GID_BYTES + DebuggerConstants.THREAD_LUID_BYTES];
        this.fromUUID(array, uuid);
        return array;
    }
    
    public void fromUUID(byte [] array, int uuid){
        int mask = 255;
        for(int i = 0; i < size; i++){
            array[i] = (byte)(uuid & mask);
            uuid >>= 8;
        }
    }
    
    public String uuid2Dotted(int uuidint){
        int lgid = uuidint & DebuggerConstants.LUID_MASK;
        int uid = (uuidint & DebuggerConstants.GID_MASK) >> 24;
        
        return(uid + "." + lgid); 
    }
    
    public int dotted2Uuid(String dotted){
        String [] dottedspec = dotted.split("[.]");
        if(dottedspec.length != 2) throw new MalformedInputException();
        int uid = Integer.parseInt(dottedspec[0]);
        int gid = Integer.parseInt(dottedspec[1]);
        uid <<= 24;
        return uid |= gid;
    }
    
    public byte dotted2MachineID(String dotted){
        String [] dottedspec = dotted.split("[.]");
        if(dottedspec.length != 2) throw new MalformedInputException();
        byte uid = Byte.parseByte(dottedspec[0]);
        return uid;
    }
    
    public String simpleMethodName(String fullMethodName){
        int dotIdx = fullMethodName.lastIndexOf(".");
        int parIdx = fullMethodName.indexOf("(");
        
        if(parIdx == -1) parIdx = fullMethodName.length()+1;
        
        return fullMethodName.substring(dotIdx + 1, parIdx - 1);
    }
    
    public Set <String> toSet(String [] stuff){
        Set <String> newSet = new HashSet <String> ();
        for(String s : stuff) newSet.add(s);
        return newSet;
    }
}
