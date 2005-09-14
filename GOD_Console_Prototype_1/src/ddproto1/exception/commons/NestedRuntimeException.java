/*
 * Created on Sep 27, 2004
 * 
 * file: RuntimeWrapCapableException.java
 */
package ddproto1.exception.commons;

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
public class NestedRuntimeException extends RuntimeException{
    /**
     * 
     */
    private static final long serialVersionUID = 3256446923350489136L;
    private List s;
    private Exception wrap;
    
    public NestedRuntimeException(String s) { super(s); }
    public NestedRuntimeException(List   s) { this.s = s; }
    public NestedRuntimeException(String s, Exception e) { super(s); this.wrap = e; }
    public NestedRuntimeException(Exception e){ super(); this.wrap = e;}
    
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
