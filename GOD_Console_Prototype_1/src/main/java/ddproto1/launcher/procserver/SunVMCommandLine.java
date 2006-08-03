package ddproto1.launcher.procserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import ddproto1.configurator.AttributeStore;
import ddproto1.configurator.IObjectSpec;
import ddproto1.configurator.IServiceLocator;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.NestedRuntimeException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.IJVMCommandLine;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;

public class SunVMCommandLine implements IJVMCommandLine {
	
    private static final Logger logger = 
        MessageHandler.getInstance().getLogger(SunVMCommandLine.class);
    
	/** Debug mode options for the Sun JVM */
	public static final String DEBUGMODE = "debug-mode";
	public static final String DEBUG_SERVERMODE = "server-mode";
	public static final String DEBUG_SUSPEND_ON_START = "suspend-on-start";
	public static final String DEBUG_ADDRESS = "global-agent-address";
	public static final String DEBUG_PORT = "jdwp-port";
    public static final String TRANSPORT = "transport";
	
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	
	/** Classpath parameters. */
	public static final String CLASS_PATH = "classpath";
	
	/** Class to run. */
	public static final String MAIN_CLASS = "main-class";
	
	/** Custom options. */
	public static final String OTHER_VMOPTIONS = "vm-options";
	
	/** Application options. */
	public static final String APPLICATION_OPTIONS = "application-options";
    
    public static final String TRANSFORMATION_AGENT_JAR = "transformation-agent-jar";
	
	private final AttributeStore attributes = new AttributeStore();
	
	private final List<String> vmParameters = new ArrayList<String>();
	private final List<String> appParameters = new ArrayList<String>();
	private final List<String> classpathElements = new ArrayList<String>();
    
    public SunVMCommandLine(){
        attributes.declareAttribute(DEBUGMODE);
        attributes.declareAttribute(DEBUG_SERVERMODE);
        attributes.declareAttribute(DEBUG_SUSPEND_ON_START);
        attributes.declareAttribute(DEBUG_ADDRESS);
        attributes.declareAttribute(DEBUG_PORT);
        attributes.declareAttribute(TRANSPORT);
        attributes.declareAttribute(JVM_LOCATION);
        attributes.declareAttribute(CLASS_PATH);
        attributes.declareAttribute(MAIN_CLASS);
        attributes.declareAttribute(OTHER_VMOPTIONS);
        attributes.declareAttribute(APPLICATION_OPTIONS);
        attributes.declareAttribute(TRANSFORMATION_AGENT_JAR);
    }
    
    private void addFromConfig(){
        IServiceLocator locator;
        try {
            locator = (IServiceLocator) Lookup.serviceRegistry().locate(
                    IConfigurationConstants.SERVICE_LOCATOR);
        } catch(IllegalStateException ex) { 
            return; 
        } catch (NoSuchSymbolException ex) {
            return;
        }

        IObjectSpec _this = locator.getMetaobject(this);
        /** No metaobject - we weren't built from the configurator. */
        if(_this == null)
        	return;
        List<IObjectSpec> params = _this.getChildrenOfType(JVM_PARAMETER);
        
        for(IObjectSpec param : params){
            try{
                String type = param.getType().getConcreteType();
                String val = param.getAttribute(ELEMENT_ATTRIB);
                
                if(type.equals(CLASSPATH_ELEMENT))
                    classpathElements.add(val);
                else if(type.equals(APPLICATION_ELEMENT))
                    appParameters.add(val);
                else if(type.equals(JVM_ARGUMENT))
                	vmParameters.add(val);
                else
                    logger.warn("Unrecognized element " + type);
                
            }catch(AttributeAccessException ex){
                logger.warn("Ignoring badly-configured element " + param);
            }
        }
        
    }
	
	public String[] renderCommandLine()
		throws AttributeAccessException
	{
		List<String> cmdLine = new ArrayList<String>();
		String location = getAttribute(JVM_LOCATION);
		if(!location.equals("") && !location.endsWith(File.separator))
			location+=File.separator;
		cmdLine.add(location + "java");
        
        String transformationAgent = 
            getAttribute(TRANSFORMATION_AGENT_JAR);
        if(!transformationAgent.equals("")){
            cmdLine.add("-javaagent:" + transformationAgent);
        }
            
        
		if(isTrue(DEBUGMODE)){
			cmdLine.add("-Xdebug");
			StringBuffer modeLine = new StringBuffer();
			modeLine.append("-Xrunjdwp:");
			modeLine.append("transport=");
            modeLine.append(getAttribute(TRANSPORT));
			modeLine.append(",server=");
			
			if(isTrue(DEBUG_SERVERMODE))
				modeLine.append("y");
			else
				modeLine.append("n");
            
            modeLine.append(",suspend=");
            if(isTrue(DEBUG_SUSPEND_ON_START))
                modeLine.append("y");
            else
                modeLine.append("n");
            
			modeLine.append(",address=");
			modeLine.append(getAttribute(DEBUG_ADDRESS));
			modeLine.append(":");
			modeLine.append(getAttribute(DEBUG_PORT));
			cmdLine.add(modeLine.toString());
		}

        this.addFromConfig();
        
        String cPath = buildClasspath();
        if(!cPath.equals("")){
            cmdLine.add("-cp");
            cmdLine.add(cPath);
        }

		
		addTokenized(cmdLine, getAttribute(OTHER_VMOPTIONS, false));
		cmdLine.addAll(vmParameters);
		
		cmdLine.add(getAttribute(MAIN_CLASS));
	
		addTokenized(cmdLine, getAttribute(APPLICATION_OPTIONS, false));
		cmdLine.addAll(appParameters);
		
		return cmdLine.toArray(new String[cmdLine.size()]);
	}
	
	public void setMainClass(String classname)
    {
        try{
            setAttribute(MAIN_CLASS, classname);
        }catch(AttributeAccessException ex){
            throw new NestedRuntimeException("Internal error.", ex);
        }
	}
	
	public String renderStringCommandLine() throws AttributeAccessException{
		StringBuffer buffer = new StringBuffer();
		for(String element : renderCommandLine()){
			buffer.append(element);
			buffer.append(" ");
		}
		
		return buffer.toString().trim();
	}

	
	private String buildClasspath() throws IllegalAttributeException, UninitializedAttributeException{
		StringBuffer cPath = new StringBuffer();
		String basePath = getAttribute(CLASS_PATH, false);
		
		if(basePath != null){
            cPath.append(basePath);
            cPath.append(File.pathSeparatorChar);
        }

        for(String cPathElement : classpathElements){
			cPath.append(cPathElement);
            cPath.append(File.pathSeparatorChar);
		}
        
        String resultingPath;
        
        if(classpathElements.size() > 0)
            resultingPath = cPath.substring(0, cPath.length()-1);
        else
            resultingPath = cPath.toString();
		
        return resultingPath;
	}
	
	private void addTokenized(List<String> valueList, String value){
		if(value == null) return;
		StringTokenizer strtok = 
			new StringTokenizer(value);
		
		while(strtok.hasMoreElements())
			valueList.add(strtok.nextToken());
	}
	
	private boolean isTrue(String attribute)
		throws AttributeAccessException
	{
        String val = null;
        try{
            val = getAttribute(attribute);
        }catch(UninitializedAttributeException ex) { return false; }
		if(val.equals(TRUE)) return true;
		else if(val.equals(FALSE)) return false;
		throw new InvalidAttributeValueException();
	}
	
	public String getAttribute(String key)
        throws IllegalAttributeException, UninitializedAttributeException
	{
		return getAttribute(key, true);
	}

	public String getAttribute(String key, boolean throwIfAbsent)
			throws IllegalAttributeException, UninitializedAttributeException {

		try {
			return attributes.getAttribute(key);
		} catch (UninitializedAttributeException ex) {
			if (!throwIfAbsent)
				return null;
			throw ex;
		}
	}

	public void setAttribute(String key, String val)
        throws IllegalAttributeException, InvalidAttributeValueException{
		attributes.setAttribute(key, val);
	}

	public boolean isWritable() {
		return true;
	}

	public void addVMParameter(String parameter) {
		vmParameters.add(parameter);
	}

	public void addApplicationParameter(String parameter) {
		appParameters.add(parameter);
	}

	public void addClasspathElement(String element) {
		classpathElements.add(element);
	}
}
