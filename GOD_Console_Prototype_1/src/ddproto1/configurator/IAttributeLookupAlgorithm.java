/*
 * Created on Aug 27, 2005
 * 
 * file: IAttributeLookupAlgorithm.java
 */
package ddproto1.configurator;

public interface IAttributeLookupAlgorithm{
    public Iterable<IObjectSpec> iterableWithRoot(IContextSearchable root);
}
