/*
 * Created on Sep 27, 2004
 * 
 * file: WrapCapableException.java
 */
package ddproto1.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author giuliano
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NestedException extends Exception{
    /**
     * 
     */
    private static final long serialVersionUID = 3258413911048140592L;
    private List s;
    private Exception wrap;
    
    public NestedException(String s) { super(s); }
    public NestedException(List   s) { this.s = s; }
    public NestedException(String s, Exception e) { super(s); this.wrap = e; }
    
    public void printStackTrace(){
        this.printStackTrace(System.err);
    }
    
    
    public void printStackTrace(PrintStream s) {
        PrintWriter pw = new PrintWriter(s);
        this.printStackTrace(pw);
    }
    
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if(s != null){
            pw.print("Wrapped exception:");
            pw.println(" **** Relevant info: ****");
            Iterator it = s.iterator();
            while(it.hasNext()) System.out.println((String)it.next());
        
            pw.println(" **** Stacktrace ****");
        }
        
        if(wrap != null){
            wrap.printStackTrace(pw);
        }
    }
}
