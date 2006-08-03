/*
 * Created on Jul 13, 2006
 * 
 * file: IProcessEventAdapter.java
 */
package ddproto1.launcher.procserver;

/**
 * In the spirit of SWT event adapters, this class provides empty 
 * implementations of event handling methods for interface 
 * IProcessEventListener. Clients should subclass and override 
 * methods as required.
 * 
 * @author giuliano
 *
 */
public class ProcessEventAdapter implements IProcessEventListener {

    public void notifyProcessKilled(int exitValue) { }

    public void notifyNewSTDOUTContent(String data) { }

    public void notifyNewSTDERRContent(String data) { }

}
