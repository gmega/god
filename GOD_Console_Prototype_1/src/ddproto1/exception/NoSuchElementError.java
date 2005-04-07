/*
 * Created on Oct 18, 2004
 * 
 * file: NoSuchElementError.java
 */
package ddproto1.exception;

/**
 * @author giuliano
 *
 */
public class NoSuchElementError extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 4049918259925037367L;

    public NoSuchElementError(String s){ super(s); }
}
