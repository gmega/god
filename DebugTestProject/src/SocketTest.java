import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

/*
 * Created on Nov 30, 2005
 * 
 * file: SocketTest.java
 */

public class SocketTest {
    public static void main(String [] args) throws IOException{
        final ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(8080);
        Thread t = new Thread(new Runnable(){
            public void run() {
                try{
                    ss.accept();
                }catch(IOException ex) { throw new RuntimeException(ex);}
            }
        });
        
        t.start();
        t.interrupt();
    }
}   
