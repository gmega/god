/*
 * Created on Sep 27, 2004
 * 
 * file: IResolutionListener.java
 */
package ddproto1.debugger.request;

/**
 * @author giuliano
 *
 */
public interface IResolutionListener {
    public void notifyResolution(IDeferrableRequest source, Object byproduct);
}
