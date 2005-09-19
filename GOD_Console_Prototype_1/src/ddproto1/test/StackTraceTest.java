/*
 * Created on Sep 19, 2005
 * 
 * file: StackTraceTest.java
 */
package ddproto1.test;

public class StackTraceTest {
    public static void main(String [] args){
        StackTraceTest stt = new StackTraceTest();
        stt.init('c');
    }
    
    public static void init(String par){
        Throwable t = new Throwable();
        Exception e = new Exception();
        StackTraceElement [] stack = t.getStackTrace();
        for(StackTraceElement element : stack){
            System.out.println(element);
        }
        t.printStackTrace();
        e.printStackTrace();
        throw new RuntimeException();
    }
    
    public void init(Character aChar){
        StackTraceTest.init(Character.toString(aChar));
    }
}
