/*
 * Created on Jul 10, 2006
 * 
 * file: DelayedResult.java
 */
package ddproto1.util;

import java.util.concurrent.FutureTask;

public class DelayedResult<V> extends FutureTask <V>{
    public DelayedResult() { super(new Runnable() { public void run() {} }, null); }
    public synchronized void set(V value) { super.set(value); }
    public synchronized void setException(Throwable t) { super.setException(t); }
}
