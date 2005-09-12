/*
 * Created on Jul 29, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Token.java
 */

package ddproto1.lexer;

/**
 * @author giuliano
 *
 */
public class Token {
    
    public static final int EMPTY = -3;
    public static final int UNIDENTIFIED = -5;
    public static final int BREAKPOINT = 0;
    public static final int METHOD = 1;
    public static final int MACHINE_ID = 2;
    public static final int STEP = 3;
    public static final int RUN = 4;
    public static final int KILL = 5;
    public static final int EXIT = 6;
    public static final int EVAL = 24;
    public static final int METHOD_BREAKSPEC = 7;
    public static final int CLASS_BREAKSPEC = 23;
    public static final int INFO = 8;
    public static final int LIST = 9;
    public static final int ALL  = 10;
    public static final int LAUNCH = 11;
    public static final int ATTACH = 12;
    public static final int SELECT = 13;
    public static final int THREAD = 14;
    public static final int HEX_ID = 15;
    public static final int SHOW = 16;
    public static final int NUMBER = 17;
    public static final int WORD = 18;
    public static final int RUNSCRIPT = 19; 
    public static final int SUSPEND = 20;
    public static final int RESUME = 21;
    public static final int DOTTED_ID = 22;
        
    public int type;
    public String text;
    
    public Token(int tokentype) { 
        this.type = tokentype;
    }
    
    public Token (int tokentype, String tokentext){
        this.type = tokentype;
        this.text = tokentext;
    }
}
