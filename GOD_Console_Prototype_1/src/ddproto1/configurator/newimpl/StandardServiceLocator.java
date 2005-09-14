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

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.exception.IncarnationException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;

public class StandardServiceLocator implements IServiceLocator{
    private static StandardServiceLocator instance;
    
    private StandardServiceLocator() { }
	
	/*
	 * This class does not prevent the incarnation from being garbage-collected. It does, 
	 * however, provide temporal consistency.
	 */
	private Map <IConfigurable, IObjectSpec> object2spec = new WeakHashMap<IConfigurable, IObjectSpec>();
	private Map <IObjectSpec, WeakReference<IConfigurable>> spec2object = new HashMap<IObjectSpec, WeakReference<IConfigurable>>();
	    
    public synchronized static StandardServiceLocator getInstance(){
        return (instance == null)?instance = new StandardServiceLocator():instance;
    }

    public IConfigurable incarnate(IObjectSpec spec)
        throws IncarnationException
    {
        return this.incarnate(spec, true);
    }
    
	public IConfigurable incarnate(IObjectSpec spec, boolean createNew)
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
		if (spec2object.containsKey(spec) && !createNew) {
			iconf = spec2object.get(spec).get();
			// Double check since it's a weak reference.
			if (iconf != null)
				return iconf;
		}
        
		IObjectSpecType type = spec.getType();
        
        String klass = null;
        
        try{
            klass = type.getConcreteType();
        }catch(IllegalAttributeException ex){
            throw new IncarnationException("This specification is not incarnable.");
        }
        
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		/* Acquires a reference to the declared class. */
		Class concreteClass = null;
		try{
			concreteClass = cl.loadClass(klass);
		}catch(ClassNotFoundException e) {
			throw new IncarnationException(e);
		}

        /* Checks if this class implements all intended interfaces and/or inherits from all
         * required supertypes.
         */
		if (!IConfigurable.class.isAssignableFrom(concreteClass))
			throw new IncarnationException(
					"Incarnations must implement interface ddproto1.configurator.IConfigurable");

        for(Class requiredParent : spec.getType().getSupportedInterfaces()){
            if(!requiredParent.isAssignableFrom(concreteClass)){
                throw new IncarnationException(
                        "Class "
                                + concreteClass
                                + " does not implement or inherit from required interface/class "
                                + requiredParent);
            }
        }
        
		/* Acquires a reference to a no-arg constructor */
		Constructor cons;
		try {
			cons = concreteClass.getDeclaredConstructor(new Class[] {});
            if(!cons.isAccessible()) cons.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new IncarnationException(
					"Incarnations must have a default constructor.");
		}

		/* Creates a new instance of the declared class. */
        try {
            iconf = (IConfigurable) cons.newInstance(new Object[] {});
        } catch (Exception e) {
            throw new IncarnationException(e);
        }

        /*
         * There will be trouble if another thread starts modifying the object
         * type after we've acquired the attribute set.
         */
        for (String key : spec.getAttributeKeys()) {
            try {
                iconf.setAttribute(key, spec.getAttribute(key));
            } catch (IllegalAttributeException ex) {
                throw new IncarnationException(
                        "Either the metaobject of specification type "
                                + type
                                + " or its incarnation reports supporting an attribute ( "
                                + key
                                + " ) it does not understand (concurrent modification?)");
            } catch (UninitializedAttributeException ex) {
                throw new IncarnationException("Required attribute " + key
                        + " for configurable " + klass
                        + " has not been set properly.");
            } catch(InvalidAttributeValueException ex) {
                throw new IncarnationException("Object does not agree with specification " +
                        "with respect to valid attribute values. This could be due to an" +
                        " erroneous specification file.");
            }
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

