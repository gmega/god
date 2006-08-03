/*
 * Created on Nov 28, 2005
 * 
 * file: IStreamCapableTunnel.java
 */
package ddproto1.launcher;

import java.io.IOException;

import org.eclipse.debug.core.IStreamListener;

import ddproto1.exception.InvalidStateException;

public interface IStreamManager {
    public static final int STDERR = 0;
    public static final int STDOUT = 1;
    
    public static final int CHARACTER = 0;
    public static final int TIME = 2;
    public static final int QUANTITY = 4;
            
    public void    addStreamListener    (IStreamListener listener);
    public boolean removeStreamListener (IStreamListener listener);
    
    public String getStderrContents();
    public String getStdoutContents();    
    
    public void writeToStdin(String message) throws InvalidStateException, IOException;
}
