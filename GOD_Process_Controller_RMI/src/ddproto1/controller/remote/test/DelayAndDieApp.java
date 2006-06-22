/*
 * Created on Jun 18, 2006
 * 
 * file: DelayAndDieApp.java
 */
package ddproto1.controller.remote.test;

public class DelayAndDieApp {
    public static void main(String args[]){
        System.out.print("Stdout: " + args[0]);
        System.err.print("Stderr: " + args[0]);
        
        try{
            Thread.sleep(Long.parseLong(args[1]));
        }catch(InterruptedException ex){
            
        }
        
        System.exit(Integer.parseInt(args[2]));
    }

}
