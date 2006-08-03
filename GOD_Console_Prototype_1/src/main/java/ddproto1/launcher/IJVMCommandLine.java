package ddproto1.launcher;

/**
 * Specialized command line for Java Virtual Machines. Adds convenience
 * methods for specifying specialized parameters, although these might
 * be specified via the IConfigurable methods as well.
 * 
 * @author giuliano
 *
 */
public interface IJVMCommandLine extends ICommandLine{
    public static final String JVM_PARAMETER = "jvm-parameter";
    
    public static final String CLASSPATH_ELEMENT = "classpath-element";
    public static final String APPLICATION_ELEMENT = "application-argument";
    public static final String JVM_ARGUMENT = "jvm-argument";    
    
    public static final String ELEMENT_ATTRIB = "element";
    public static final String JVM_LOCATION = "jvm-location";
    
	public void setMainClass(String classname);
	
	public void addVMParameter(String parameter);
	
	public void addClasspathElement(String element);
}
