/*
 * Created on Apr 12, 2005
 * 
 * file: StandardServiceLocator.java
 */
package ddproto1.configurator.newimpl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import ddproto1.configurator.IConfigurable;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.IncarnationException;

public class StandardServiceLocator implements IServiceLocator{
    private static StandardServiceLocator instance;
    
    private StandardServiceLocator() { }
	
	/*
	 * This class does not prevent the incarnation from being garbage-collected. It does, 
	 * however, provide temporal consistency.
	 */
	private Map <IConfigurable, IObjectSpec> object2spec = new WeakHashMap<IConfigurable, IObjectSpec>();
	private Map <IObjectSpec, WeakReference<IConfigurable>> spec2object = new HashMap<IObjectSpec, WeakReference<IConfigurable>>();
	    
    public synchronized StandardServiceLocator getInstance(){
        return (instance == null)?instance = new StandardServiceLocator():instance;
    }

	public IConfigurable incarnate(IObjectSpec spec)
			throws IncarnationException {
		/*
		 * incarnate guarantees a consistent temporal view of all IObjectSpec
		 * incarnations. This means that, at any given point in time:
		 * 
		 * Let objectSpec1 and objectSpec2 be two refereces to an instance of
		 * an IObjectSpec. If configurable1 and configurable2 are two instances
		 * obtained through calls to incarnate(objectSpec1) and incarnate(objectSpec2)
		 * respectively, then:
		 * 
		 * configurable1 == configurable2 <=> objectSpec1 == objectSpec2
		 * 
		 * Note that this does not mean that incarnate always returns the same reference. 
		 * The guarantee is only with respect to time.
		 */
		IConfigurable iconf;
		if (spec2object.containsKey(spec)) {
			iconf = spec2object.get(spec).get();
			// Double check since it's a weak reference.
			if (iconf != null)
				return iconf;
		}

		IObjectSpecType type = spec.getType();
		String klass = type.getConcreteType();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		/* Acquires a reference to the declared class. */
		Class concreteClass = null;
		try{
			concreteClass = cl.loadClass(klass);
		}catch(ClassNotFoundException e) {
			throw new IncarnationException(e);
		}
		
		if (!IConfigurable.class.isAssignableFrom(concreteClass))
			throw new IncarnationException(
					"Incarnations must implement interface ddproto1.configurator.IConfigurable");

		/* Acquires a reference to a no-arg constructor */
		Constructor cons;
		try {
			cons = concreteClass.getConstructor(new Class[] {});
		} catch (NoSuchMethodException e) {
			throw new IncarnationException(
					"Incarnations must have a public default constructor.");
		}

		/* Creates a new instance of the declared class. */
		cons.setAccessible(true);
		try {
			iconf = (IConfigurable) cons.newInstance(new Object[] {});
		} catch (Exception e){
			throw new IncarnationException(e);
		}

		/* Object initialization must be atomic */
		type.lockForReading();
		try {
			for (String key : type.attributeSet()) {
				iconf.setAttribute(key, spec.getAttribute(key));
			}
		} catch (IllegalAttributeException ex) {
			throw new IncarnationException(
					"Either the object or the metaobject reports supporting "
							+ "an attribute it does not understand.");
		} finally {
			type.unlock();
		}

		/* Updates internal tables. */
		spec2object.put(spec, new WeakReference<IConfigurable>(iconf));
		object2spec.put(iconf, spec);
		
		return iconf;
	}
	
	public IObjectSpec getMetaobject(Object self){
		return object2spec.get(self);
	}
}	

