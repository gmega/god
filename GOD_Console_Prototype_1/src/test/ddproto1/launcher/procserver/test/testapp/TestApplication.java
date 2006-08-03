/*
 * Created on Jul 10, 2006
 * 
 * file: TestApplication.java
 */
package ddproto1.launcher.procserver.test.testapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestApplication {
    
    private static final int BUFFER_SIZE = 100;
    
    public static void main(String [] args) throws Exception{
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String inputLine = stdin.readLine();
        if(!inputLine.equals("giuliano needs beer")){
            System.err.println("Error.");
            System.exit(1);
        }
        
        BufferedReader out = new BufferedReader(new InputStreamReader(
                TestApplication.class.getClassLoader().getResourceAsStream(
                        "test.text")));
        
        /** Outputs the text alternating between stderr and stdout. 
         * Note: Not anymore. We output only to stdout to ensure
         * ordering, which I still don't know if is 'ensured'.*/
        String line;
        char [] readBuffer = new char[BUFFER_SIZE];
        try{
            int size = 0;
            while((size = out.read(readBuffer)) != -1){
                System.out.print(new String(readBuffer, 0, size));
            }
        }catch(Throwable t){
            t.printStackTrace();
        }
    }
}
