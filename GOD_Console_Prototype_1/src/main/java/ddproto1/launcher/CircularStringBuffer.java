/*
 * Created on Nov 28, 2005
 * 
 * file: CircularStringBuffer.java
 */
package ddproto1.launcher;

public class CircularStringBuffer {
    
    private char[] buffer;
    int end = 0;
    boolean round = false;
    
    public CircularStringBuffer(int capacity){
        buffer = new char[capacity];
    }
    
    public void append(String s){
        char [] _s = s.toCharArray();
        int isPosition = Math.max(0, _s.length - buffer.length);
        
        for(int i = isPosition; i < _s.length; i++){
            buffer[end] = _s[i];
            
            /** Detects when the buffer overflows, 
             * hopefully with little overhead. */
            if(!round){ if((end + 1) == buffer.length) round = true; }
            
            end = (end + 1) % buffer.length; 
        }
        
    }
    
    public String toString(){
        int begin = end, length = buffer.length;
        
        if(!round){ length = end; begin = 0; } 
                
        char[] cBuffer = new char[length];
        for(int i = 0; i < length; i++)
            cBuffer[i] = buffer[((begin + i) % buffer.length)];
        
        System.out.println(new String(cBuffer));
        System.out.println("Contents: " + new String(buffer));
        return new String(cBuffer);
    }
}
