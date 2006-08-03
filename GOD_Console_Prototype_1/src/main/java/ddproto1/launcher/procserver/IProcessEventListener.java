package ddproto1.launcher.procserver;

public interface IProcessEventListener {
	public void notifyProcessKilled(int exitValue);
	public void notifyNewSTDOUTContent(String data);
	public void notifyNewSTDERRContent(String data);
}
