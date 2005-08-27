/*
 * Created on Aug 27, 2005
 * 
 * file: IAttributeLookupAlgorithm.java
 */
package ddproto1.configurator.newimpl;

public interface IAttributeLookupAlgorithm{
    public Iterable<IObjectSpec> iterableWithRoot(IContextSearchable root);
}
