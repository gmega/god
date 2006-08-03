package ddproto1.launcher.procserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.ConfigException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.ICommandLine;
import ddproto1.util.Lookup;

public class PyExpectSSHExecutor implements IRemoteCommandExecutor, IConfigurable {

    public static final String PYTHON_INTERPRETER = "python-interpreter";
	public static final String PEXPECT_SCRIPT = "pexpect-script";
	public static final String REMOTE_HOST = IConfigurationConstants.LOCAL_AGENT_ADDRESS;
	public static final String SSH_PORT = "ssh-port";
	public static final String USER = "user";
	public static final String PASSWORD = "password";
	
	private final Map <String, String> attributeMap = 
		Collections.synchronizedMap(new HashMap <String, String> ());
    
    private ICommandLine cLine;
    
    public PyExpectSSHExecutor(){
        synchronized(this){
            cLine = null;
        }
    }
	
	public Process executeRemote() 
		throws ConfigException, IOException
	{
        String [] commandString; 
        ArrayList<String> commandList = new ArrayList<String>();
        
        try{
            commandString = getCommandLine().renderCommandLine();
            commandList.add(getAttribute(PYTHON_INTERPRETER));
            commandList.add(getAttribute(PEXPECT_SCRIPT));
            commandList.add("--password=" + getAttribute(PASSWORD));
            commandList.add("--host=" + getAttribute(REMOTE_HOST));
            commandList.add("--user=" + getAttribute(USER));
		    commandList.add("--port=" + getAttribute(SSH_PORT));
        }catch(AttributeAccessException ex){
            throw new ConfigException(ex);
        }
		
        /** Ideal application for the strategy pattern (one 
         * strategy per shell), but I don't know if I need 
         * this flexibility yet.
         */
		escapeRemoteCommands(commandString);
        
        String [] fullCommandline = 
			new String[commandList.size() + commandString.length];
		
		commandList.toArray(fullCommandline);
		System.arraycopy(commandString, 0, fullCommandline, commandList.size(), commandString.length);
        for(String garb : fullCommandline) System.out.print(garb + ", ");
		return Runtime.getRuntime().exec(fullCommandline);
	}
    
    private void escapeRemoteCommands(String [] commands){
        for(int i = 0; i < commands.length; i++){
            String paddedCommand = commands[i].replaceAll(" ", "\\\\ ");
            commands[i] = paddedCommand;
        }
    }

	public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
		if(!attributeMap.containsKey(key))
			throw new IllegalAttributeException();
		
		return attributeMap.get(key);
	}

	public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
		attributeMap.put(key, val);
	}

	public boolean isWritable() {
		return true;
	}
    
    public ICommandLine getCommandLine() throws ConfigException{
        synchronized(this){
            if(cLine != null) return cLine;
        }
        try{
            IServiceLocator sl = (IServiceLocator)Lookup.serviceRegistry().locate(IConfigurationConstants.SERVICE_LOCATOR);
            ICommandLine cl = (ICommandLine)sl.incarnate(sl.getMetaobject(this).getChildSupporting(ICommandLine.class));
            synchronized(this){
                cLine = cl;
            }
            return cLine;
        }catch(Exception ex){
            throw new ConfigException(ex);
        }
    }

    public synchronized void setCommandLine(ICommandLine cLine){
        this.cLine = cLine;
    }
}
