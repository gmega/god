import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/*
 * Created on 01/04/2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
/**
 * @author giuliano
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SimpleClass {
	
	private static int x = 1;
	private static volatile boolean go = false;
	
	private static ThreadLocal tl = new ThreadLocal();
	
	public static void main(String[] args) throws Exception{
        
        Map<String, String> mp = new HashMap<String, String>();
        System.out.println(StringBuilder.class.getName()); // Forces load of StringBUilder
        mp.put("Hello", "world");
        mp.put("I am", "Giuliano");
        
        Runnable tRunnable = new Runnable(){
            public void run(){
                for(int i = 0; i < 10; i++){
                    String toPrint = "Line is " + i;
                    doPrint(toPrint);
                    doPrint(toPrint);
                }
            }
            
            private void doPrint(String s){
                System.out.println(s);
            }
        };
        
        tRunnable.run();

		int j = -5;
		long lj = 0;
		lj = lj | j;
		System.out.println("Cast - " + lj);
		
		overloadedMethod(3);
		overloadedMethod("Giuliano", new Object[]{ "is doing this." });
		
		tl.set("It's the end of the world as we know it.");
        
        final B b = new B();
		
		Runnable r1 = new Runnable(){
            public void run() {
                b.doSomething();
                while(!go);
                overloadedMethod(1);
            }
		};
        
        Object o = new Object();
        synchronized(o){
            o.wait(1000);
        }
	
        try{
            throw new RuntimeException();
        }catch(RuntimeException e){
            System.out.println("oi");
        }
		
		Runnable r2 = new Runnable(){
            public void run() {
                b.doSomethingElse();
                go = true;
                overloadedMethod(2);
            }
		};
		
		ThreadClass cu = new ThreadClass();
		cu.run();
		
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);
        
		System.out.println("Will start t1");
		t1.start();
        
        System.out.println("Will start t2");
		t2.start();
        
        /** Classloader stuff. */
        ClassLoader cl1 = new URLClassLoader(new URL[] {new URL("file:/home/giuliano/workspace/stuff/")}, null);
        ClassLoader cl2 = new URLClassLoader(new URL[] {new URL("file:/home/giuliano/workspace/stuff/")}, null);
        
        Class dummyClass_1 = cl1.loadClass("DummyClass");
        
        if(dummyClass_1 == DummyClass.class) throw new Exception("Not the expected behavior");
        
        Method hurtMe_1 = dummyClass_1.getMethod("hurtMe", new Class[] {});
        
        Object hurtMe_instance_1 = dummyClass_1.newInstance();
        
        hurtMe_1.invoke(hurtMe_instance_1, new Object[]{});
        
        
        Class dummyClass_2 = cl2.loadClass("DummyClass");
//        Class dummyClass_2 = cl1.loadClass("DummyClass");

        Method hurtMe_2 = dummyClass_2.getMethod("hurtMe", new Class[] {});

        Object hurtMe_instance_2 = dummyClass_2.newInstance();

        hurtMe_2.invoke(hurtMe_instance_2, new Object[]{});
        
        System.out.println("Before calling join.");
        
        t1.join();

        System.out.println("Reached end of application.");
	}
	
	public static void overloadedMethod(int i){    
        System.out.println(i);
    }
	
	public static void overloadedMethod(String s, Object [] objs){
		System.out.println(s);
	}
	
	static class ThreadClass implements Runnable{
	    public void run(){
	        
	    }
	}
    
    static class A {
        public void doSomething(){
            synchronized(this){
                System.out.println("Did something");
            }
        }
    }
    
    static class B extends A{
        public synchronized void doSomethingElse(){
            System.out.println("Did something else");
        }
    }

}
