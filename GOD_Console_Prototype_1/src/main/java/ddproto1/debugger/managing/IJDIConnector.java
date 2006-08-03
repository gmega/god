package ddproto1.debugger.managing;

import java.io.IOException;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import ddproto1.exception.ConfigException;

public interface IJDIConnector {
	public void prepare() throws IOException, IllegalConnectorArgumentsException, ConfigException;
	public VirtualMachine connect() throws IOException, IllegalConnectorArgumentsException, ConfigException;
}
