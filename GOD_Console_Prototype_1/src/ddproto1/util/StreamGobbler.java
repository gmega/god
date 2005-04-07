/*
 * Created on Jul 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ddproto1.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ddproto1.interfaces.IStreamGobblerListener;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StreamGobbler implements Runnable{
    
    private static final String module = "StreamGobbler -";
    private static final int min_size = 50;
        
    private InputStream is;
    private Set listeners = new HashSet();
    
    public StreamGobbler(InputStream is){
        this.is = is;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        
        try{
            String line;
            while((line = br.readLine()) != null){
                Iterator it = listeners.iterator();
                while(it.hasNext())
                    ((IStreamGobblerListener)it.next()).receiveLine(line);
            }
      

        }catch(IOException e){
            MessageHandler mh = MessageHandler.getInstance();
            /* These messages just mean the remote process has died 
             * without closing its output streams (something which is 
             * very common in some OSes). 
             */
            if(e.getMessage().startsWith("Stream closed") ||
            		e.getMessage().startsWith("Bad file")){
                mh.getWarningOutput().println("Warning - debuggee I/O disconnected without issuing an end-of-stream marker.");
                return;
            }
            
            mh.getErrorOutput().println(module + " I/O exception while reading process stream");
            mh.printStackTrace(e);
        }
    }
    
    public void addStreamGobblerListener(IStreamGobblerListener sgl){
        listeners.add(sgl);
    }
}
