/*
 * Created on Sep 27, 2004
 * 
 * file: RequestProcessorException.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class RequestProcessorException extends NestedRuntimeException{
    /**
     * 
     */
    private static final long serialVersionUID = 3257565105234326576L;
    public RequestProcessorException(String s){super(s);}
    public RequestProcessorException(String s, Exception e){super(s,e);}
}
