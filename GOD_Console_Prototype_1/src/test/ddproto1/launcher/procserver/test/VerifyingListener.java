/*
 * Created on Jul 11, 2006
 * 
 * file: VerifyingListener.java
 */
package ddproto1.launcher.procserver.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import ddproto1.launcher.procserver.IProcessEventListener;

public class VerifyingListener implements IProcessEventListener{
    
    private volatile boolean bad = false;
    private volatile InputStreamReader text;
    private final CountDownLatch latch = new CountDownLatch(2);
    private AtomicInteger charCount = new AtomicInteger(0);

    public VerifyingListener(InputStreamReader testText){
        this.text = testText;
    }
    
    public void notifyProcessKilled(int exitValue) {
        try{
            /** We'll wait. */
            latch.countDown();
            latch.await();
            if(text.ready()) flagWentWrong();
            
        }catch (Exception ex) { flagWentWrong(); }
    }
    
    public void waitForDeath()
        throws InterruptedException
    {
        latch.await();
    }

    public void notifyNewSTDOUTContent(String data) {
        /** Checks that the received characters match what's 
         * in the InputStream
         */
        consumeCharacters(data);
    }

    public void notifyNewSTDERRContent(String data) {
        System.err.println("Stderr content: " + data);
        consumeCharacters(data);
    }
    
    private void flagWentWrong(){
        bad = true;
        latch.countDown();
    }
    
    public boolean wentWrong(){
        return bad;
    }

    protected void consumeCharacters(String data)
    {
        try{
            for(int i = 0; i < data.length(); i++){
                if(!text.ready()){
                    latch.countDown();
                    return;
                }
                char streamNext = (char)text.read();
                charCount.incrementAndGet();
                if(!text.ready()){
                    System.err.println("Signalling countdown latch.");
                    latch.countDown();
                    break;
                }
                if(streamNext != data.charAt(i))
                    flagWentWrong();
            }
        }catch(Throwable ex){
            ex.printStackTrace();
            flagWentWrong();
        }
        
        System.err.println(charCount.intValue());
    }
}
