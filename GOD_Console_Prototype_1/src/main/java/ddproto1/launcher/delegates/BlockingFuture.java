package ddproto1.launcher.delegates;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class BlockingFuture implements InvocationHandler {

	private Object delegate;
	
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		synchronized(this){
			while(delegate == null){
				try{
					this.wait();
				}catch(InterruptedException ex) { }
			}
		}
		
		Class klass = delegate.getClass();
		Method m = klass.getMethod(method.getName(), 
				(Class[])method.getParameterTypes());
		return m.invoke(delegate, args);
	}
	
	public synchronized void setDelegate(Object delegate){
		this.delegate = delegate;
		this.notifyAll();
	}

}
