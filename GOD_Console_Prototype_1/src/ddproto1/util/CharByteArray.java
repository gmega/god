/*
 * Created on Sep 10, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: CharByteArray.java
 */

package ddproto1.util;

/**
 * @author giuliano
 *
 */
public class CharByteArray {
    private static final char [] masks = new char[] {255, 65280};
    private static final int [] shifts = new int [] {0, 8};
    private static final int NSIGMASK = 127;
    private static final int SIGMASK = 128;
    
    public char [] array;
    private byte [] CharByteArray;
    private int length;
    
    private CharByteArray() { }
    
    /** 
     * Allocates a byte array of size nBytes if nBytes is even or nBytes + 1 if nBytes
     * is odd (this ammounts to saying that only even-length arrays are allowed).
     * 
     * @param nBytes
     */
    public CharByteArray(int nBytes){
        if(nBytes <= 0) throw new IllegalArgumentException();
        length = nBytes;
        if(nBytes % 2 == 1) nBytes++;
        array = new char[nBytes/2];
        for(int i = 0; i < nBytes/2; i++){
            array[i] = '\0';
        }
    }
    
    public CharByteArray(char [] contents){
        array = contents;
        this.length = contents.length*2;
    }
    
    public CharByteArray(byte [] contents){
        int length = contents.length;
        this.length = length;
        if(length % 2 == 1) length++;
        array = new char[length/2];
        
        this.writeAt(0, contents);
    }
    
    public CharByteArray insert(byte [] excerpt, int pByte){
        
        if(pByte >= length) throw new ArrayIndexOutOfBoundsException();
        
        int idx = pByte/2;
        int byt = pByte%2;
        
        //boolean startsHalf = (byt == 1)?true:false;
        //boolean endsHalf = startsHalf ^ (excerpt.length%2 == 1);
        
        int newLength = excerpt.length + length;
        CharByteArray nArray = new CharByteArray(newLength);
        System.arraycopy(array, 0, nArray.array, 0, idx+1);
        
        for(int i = pByte; i < (pByte + excerpt.length); i++){
            nArray.writeAt(i, excerpt[i-pByte]);
        }

        for(int i = pByte + excerpt.length; i < excerpt.length + length; i++){
            nArray.writeAt(i, this.get(i - excerpt.length));
        }
        
        CharByteArray = null;
        
        return nArray;
    }
    
    public CharByteArray subarray(int start){
        return this.subarray(start, length);
    }
    
    public CharByteArray subarray(int start, int end){
        int length = start - end + 1;
        CharByteArray sub = new CharByteArray(length);
        
        for(int i = 0; i < length; i++)
            sub.writeAt(i, this.get(start+i));
        
        return sub;
    }
    
    public void writeAt(int pByte, byte [] valarray){
        if(valarray.length + pByte >= length) 
            throw new ArrayIndexOutOfBoundsException();
        
        for(int i = pByte; i < valarray.length + pByte; i++)
            writeAt(i, valarray[i]);
        
        CharByteArray = null;
    }
    
    public void writeAt(int pByte, byte val){
        if(pByte >= length) throw new ArrayIndexOutOfBoundsException();
        
        int idx = pByte/2;
        int byt = pByte%2;
        
        /* Saves the content of the other byte */
        char aux = (char)(array[idx] & masks[1-byt]);

        if(val >= 0)
            aux = (char)(aux | (val << shifts[byt]));
        else{
            // Removes the signal byte.
            val &= NSIGMASK;
            // ORs the signal-byte removed data in.
            aux |= (val << shifts[byt]);
            // ORs the signal-byte in, but this time it won't be cast as a signal byte.
            aux |= (SIGMASK << shifts[byt]);
        }
        array[idx] = aux;
        CharByteArray = null;
    }
    
    public byte get(int pByte){
        int idx = pByte/2;
        int byt = pByte%2;

       return (byte)((array[idx] & masks[byt]) >> shifts[byt]);
    }
    
    public int length(){
        return length;
    }
    
    public char [] getChars(){
        return array;
    }
    
    public byte [] getBytes(){
        /* Improves amortized costs if array is modified with little
         * frequency.
         */
        if(CharByteArray != null) return CharByteArray;
        
        /* Must do it like this or we could face serious alignment
         * issues.
         */
        byte [] bytearray = new byte[length];
        for(int i = 0; i < length; i++)
            bytearray[i] = this.get(i);
        
        return bytearray;
    }
    
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for(int i = 0; i < length; i++){
            sb.append(get(i));
            if(i != length - 1) sb.append(",");
        }
        
        sb.append("]");
        
        return sb.toString();
    }
    
    public static byte getByte(char c, int byt){
        return (byte)((c & masks[byt]) >> shifts[byt]); 
    }
    
    public static char asChar(byte val, int byt){
        char b = 0;
        
        /* Saves the content of the other byte */
        if(val >= 0)
            b = (char)(b | (val << shifts[byt]));
        else{
            // Removes the signal byte.
            val &= NSIGMASK;
            // ORs the signal-byte removed data in.
            b |= (val << shifts[byt]);
            // ORs the signal-byte in, but this time it won't be cast as a signal byte.
            b |= (SIGMASK << shifts[byt]);
        }
        return b;
    }
}
