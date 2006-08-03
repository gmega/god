package ddproto1.launcher.procserver.test;

import junit.framework.TestCase;
import ddproto1.launcher.procserver.SunVMCommandLine;
import ddproto1.util.MessageHandler;

public class SunVMCommandLineTest extends TestCase {
	public void testSunVMCommandLine()
		throws Exception
	{
        MessageHandler.autoConfigure();
		SunVMCommandLine scl = new SunVMCommandLine();
		
		scl.setAttribute(SunVMCommandLine.CLASS_PATH, "/home/giuliano/one.jar");
		scl.setAttribute(SunVMCommandLine.APPLICATION_OPTIONS, "--a=1 --d=2");
		scl.setAttribute(SunVMCommandLine.DEBUG_ADDRESS, "localhost");
		scl.setAttribute(SunVMCommandLine.DEBUGMODE, "true");
        scl.setAttribute(SunVMCommandLine.TRANSPORT, "dt_socket");
        scl.setAttribute(SunVMCommandLine.TRANSFORMATION_AGENT_JAR, "LocalAgent.jar");
		scl.setAttribute(SunVMCommandLine.DEBUG_PORT, "8080");
		scl.setAttribute(SunVMCommandLine.DEBUG_SERVERMODE, "true");
		scl.setAttribute(SunVMCommandLine.DEBUG_SUSPEND_ON_START, "true");
		scl.setAttribute(SunVMCommandLine.JVM_LOCATION, "/home/giuliano/bin/");
		scl.setAttribute(SunVMCommandLine.MAIN_CLASS, "cl.ExampleClass");
		
		scl.addApplicationParameter("--parameter=value");
		scl.addApplicationParameter("--parameter2=value2");
		scl.addClasspathElement("/home/giuliano/something.jar");
		scl.addClasspathElement("/home/giuliano/another.jar");
        
        String comparison = "/home/giuliano/bin/java " +
        "-javaagent:LocalAgent.jar " +
        "-Xdebug " +
        "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=localhost:8080 " +
        "-cp /home/giuliano/one.jar" +
        ":/home/giuliano/something.jar" +
        ":/home/giuliano/another.jar " +
        "cl.ExampleClass " +
        "--a=1 --d=2 --parameter=value --parameter2=value2";
        
		System.out.println(scl.renderStringCommandLine());
        System.err.println(comparison);
		assertTrue(scl.renderStringCommandLine().equals(comparison));
	}
}	
