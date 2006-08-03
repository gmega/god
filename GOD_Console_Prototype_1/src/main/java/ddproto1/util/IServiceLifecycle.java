/*
 * Created on Nov 30, 2005
 * 
 * file: IServiceLifecycle.java
 */
package ddproto1.util;

public interface IServiceLifecycle 
{
	public static final int STOPPED = 1;

	public static final int STOPPING = 2;
	
	public static final int STARTING = 3;
	
	public static final int STARTED = 4;

    public void start() throws Exception;
    
    public void stop() throws Exception;
    
    public int currentState();
}
