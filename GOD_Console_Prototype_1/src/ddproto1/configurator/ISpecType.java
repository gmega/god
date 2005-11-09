/*
 * Created on Sep 5, 2005
 * 
 * file: ISpecType.java
 */
package ddproto1.configurator;

/**
 * This interface describes a specification type. A specification type is nothing
 * more than an alias to a Java type set. 
 * 
 * @author giuliano
 *
 */
public interface ISpecType {
    /**
     * Returns the name of this specification type.
     * 
     * @return
     */
    public String getName();
    
    /**
     * Returns the 'extension' of this specification type. The extension is like
     * a second name and its used by some parts of the system. 
     * 
     * Note: After a while, I started to realize that this 'extension' thing is not
     * really that useful. 
     * 
     * @return
     */
    public String getExtension();
    
    /**
     * Returns a list of the Java types that this specification type is supposed
     * to implement/extend.
     * 
     * @return
     */
    public String [] expectedInterfaces();
}
