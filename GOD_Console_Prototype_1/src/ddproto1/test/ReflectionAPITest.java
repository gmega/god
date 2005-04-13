package ddproto1.test;

import junit.framework.TestCase;

public class ReflectionAPITest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(JVMShellLauncherTest.class);
    }
	
	public void testHierarchy(){
		Class [] allClasses = B.class.getClasses();
		// Wrong. Doesn't tell you implemented interfaces.
		assertTrue(allClasses.length > 3);
	}
	
	public interface IA { }
	public interface IB { }
	
	public class A implements IA{ } 
	public class B extends A implements IB { }
}
