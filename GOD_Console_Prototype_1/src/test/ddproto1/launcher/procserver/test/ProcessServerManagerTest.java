package ddproto1.launcher.procserver.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.NoSuchObjectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.BasicConfigurator;

import ddproto1.configurator.commons.IConfigurable;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.controller.constants.ProcessServerConstants;
import ddproto1.controller.interfaces.IProcessServer;
import ddproto1.controller.interfaces.IRemoteProcess;
import ddproto1.controller.interfaces.LaunchParametersDTO;
import ddproto1.debugger.eventhandler.processors.PyExecCommandLine;
import ddproto1.exception.commons.AttributeAccessException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;
import ddproto1.launcher.ICommandLine;
import ddproto1.launcher.IJVMCommandLine;
import ddproto1.launcher.procserver.PyExpectSSHExecutor;
import ddproto1.launcher.procserver.IProcessEventListener;
import ddproto1.launcher.procserver.IRemoteCommandExecutor;
import ddproto1.launcher.procserver.ProcessServerManager;
import ddproto1.launcher.procserver.SunVMCommandLine;
import ddproto1.launcher.procserver.test.testapp.TestApplication;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.TestUtils;
import junit.framework.TestCase;

/**
 * 
 * This test will attempt to launch three remote process servers
 * and then perform an RMI multideath 
 * test per server.
 * 
 * @author giuliano
 *
 */
public class ProcessServerManagerTest extends TestCase 
	implements IConfigurationConstants{
	
	private static final String LOG4J_CONF_URL =
		"procservermanagertest-log4j-conf-url";
	
	private static final int LAUNCH_THREADS = 2;

	private final ProcessServerManager psManager =
		new ProcessServerManager();

	public void setUp(){
		BasicConfigurator.configure();
        MessageHandler.autoConfigure();
	}

	public void testProcessServerManager()
		throws Exception
	{
		psManager.setAttribute(CALLBACK_OBJECT_PATH, TestUtils.getProperty(CALLBACK_OBJECT_PATH));
		psManager.setAttribute(RMI_REGISTRY_PORT, TestUtils.getProperty(RMI_REGISTRY_PORT));
		
		final CyclicBarrier launchBarrier = new CyclicBarrier(LAUNCH_THREADS);
		final CyclicBarrier termBarrier = new CyclicBarrier(LAUNCH_THREADS+1);
		final AtomicInteger exceptioned = new AtomicInteger(0);
		
		Runnable starter = new Runnable(){
			public void run(){
				try{
					launchBarrier.await();
					psManager.start();
				}catch(Exception ex){
					if(!(ex instanceof IllegalStateException)){
						ex.printStackTrace();
						fail();
					}
					exceptioned.incrementAndGet();
				}finally{
                    try{ 
                        termBarrier.await(); 
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
				
			}
		};
		
		/** Use multiple threads to start the process server.
		 * Only one thread should succeed, all other threads
		 * should get exceptions.
		 */
		for(int i = 0; i < LAUNCH_THREADS; i++)
			new Thread(starter).start();
		
		try{
			termBarrier.await();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		assertTrue(exceptioned.get() == (LAUNCH_THREADS - 1));
		
		/** Launches a process server per machine. We'll use
		 * multiple threads to accomplish this. All threads
		 * should acquire the same reference, and the process
		 * server should be launched only once.
		 */
		ProcessServerAcquisitor [] acquisitors = new ProcessServerAcquisitor[TestUtils.addresses.length];
		for(int i = 0; i < TestUtils.addresses.length; i++)
			acquisitors[i] = new ProcessServerAcquisitor(TestUtils.getProperty(TestUtils.MY_ADDRESS),
                    TestUtils.getProperty(TestUtils.addresses[i]), TestUtils.getProperty(LOG4J_CONF_URL));
		
		for(ProcessServerAcquisitor acquisitor : acquisitors){
			launchBarrier.reset();
			termBarrier.reset();
			AcquireAndLaunchMultiple aclm = new AcquireAndLaunchMultiple(acquisitor,
												launchBarrier, termBarrier);
			for(int i = 0; i < LAUNCH_THREADS; i++){
				new Thread(aclm).start();
			}
			
			termBarrier.await();
			
			aclm.getServer().shutdownServer(true);
            Thread.sleep(4000); // Waits for server to die.
		}
		
		psManager.stop();

        /** We now start over and perform the actual process test.
         * This test can be tunned to be either very light or very heavy,
         * depending on the size of text used for testing.   */
        psManager.start();
        SunVMCommandLine commLine = new SunVMCommandLine();
        commLine.setAttribute(SunVMCommandLine.JVM_LOCATION, TestUtils.getProperty(SunVMCommandLine.JVM_LOCATION));
        commLine.setAttribute(SunVMCommandLine.TRANSFORMATION_AGENT_JAR, "");
        commLine.setAttribute(SunVMCommandLine.DEBUGMODE, SunVMCommandLine.FALSE);
        commLine.addClasspathElement(TestUtils.getProperty(TestUtils.PROJECT_BINARIES));
        commLine.addClasspathElement(TestUtils.getProperty(TestUtils.TEST_RESOURCES));
        commLine.setAttribute(SunVMCommandLine.MAIN_CLASS, TestApplication.class.getName());
        
        VerifyingListener vListener = new VerifyingListener(
                new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("test.text")));

        LaunchParametersDTO lpdto = new LaunchParametersDTO(commLine.renderCommandLine(), 
                                new LaunchParametersDTO.EnvironmentVariable[] { }, 42);
        
        IProcessServer ipServer = acquisitors[0].acquire();
        psManager.registerProcessListener(vListener, 42);
        
        IRemoteProcess remoteProc = ipServer.launch(lpdto);
        remoteProc.writeToSTDIN("giuliano needs beer\n");
        
        vListener.waitForDeath();
        psManager.stop();
        
        if(vListener.wentWrong())
            fail();
	}

	private class AcquireAndLaunchMultiple implements Runnable{

		private volatile ProcessServerAcquisitor acquisitor;
		private IProcessServer server;
		private volatile CyclicBarrier launch;
		private volatile CyclicBarrier term;
		
		public AcquireAndLaunchMultiple(ProcessServerAcquisitor acquisitor,
				CyclicBarrier launch, CyclicBarrier term){
			this.acquisitor = acquisitor;
			this.launch = launch;
			this.term = term;
		}
		
		public void run() {
			try{
				try{ 
					launch.await(); 
				}catch(Exception iex) { iex.printStackTrace(); }
                while(true){
                    try{
                        setServer(acquisitor.acquire());
                        break;
                    }catch(NoSuchObjectException ex){
                        Thread.sleep(5000);
                    }
                }
			}catch(Exception ex){
				ex.printStackTrace();
				fail();
			}finally{
                try{ 
                    term.await(); 
                }catch(Exception iex) { iex.printStackTrace(); }
            }
		}
		
		public synchronized void setServer(IProcessServer newRef){
			if(server == null) server = newRef;
			else assertTrue(newRef == server);
		}
		
		public synchronized IProcessServer getServer(){
			return server;
		}
		
	}
	
	private class ProcessServerAcquisitor implements ProcessServerConstants{
		private volatile String localAddress;
		private volatile String remoteAddress;
		private volatile String log4jconfurl;
				
		private PyExpectSSHExecutor executor;
		
		public ProcessServerAcquisitor(String localAddress, String remoteAddress,
				String log4jconfurl)
			throws Exception
		{
			
			PyExpectSSHExecutor commExecutor = new PyExpectSSHExecutor();
            commExecutor.setAttribute(PyExpectSSHExecutor.PYTHON_INTERPRETER,
                    TestUtils.getProperty(TestUtils.PY_INTERPRETER));
			commExecutor.setAttribute(PyExpectSSHExecutor.PEXPECT_SCRIPT, 
				TestUtils.getProperty(TestUtils.EXPECT_SCRIPT));
			commExecutor.setAttribute(PyExpectSSHExecutor.PASSWORD, TestUtils.getProperty(TestUtils.DDUSER_PASS));
			commExecutor.setAttribute(PyExpectSSHExecutor.REMOTE_HOST, remoteAddress);
			commExecutor.setAttribute(PyExpectSSHExecutor.SSH_PORT, TestUtils.getProperty(TestUtils.SSH_SERVER_PORT));
			commExecutor.setAttribute(PyExpectSSHExecutor.USER, TestUtils.getProperty(TestUtils.DDUSER_NAME));
		
			this.remoteAddress = remoteAddress;
			this.localAddress = localAddress;
			this.log4jconfurl = log4jconfurl;
			setRemoteExecutor(commExecutor);
		}
		
		public IProcessServer acquire()
			throws Exception
		{
			RemoteProcessSpec spec = new RemoteProcessSpec();
			spec.setAttribute(GLOBAL_AGENT_ADDRESS, localAddress);
			spec.setAttribute(LOCAL_AGENT_ADDRESS, remoteAddress);
            PyExpectSSHExecutor executor = getRemoteExecutor();
            executor.setCommandLine(getCommandLine());
			return psManager.getProcessServerFor(spec, executor);
		}
		
		private synchronized void setRemoteExecutor(PyExpectSSHExecutor executor){
			this.executor = executor;
		}
		
		private synchronized PyExpectSSHExecutor getRemoteExecutor(){
			return executor;
		}
        
        private synchronized ICommandLine getCommandLine()
            throws ExecutionException, InterruptedException, AttributeAccessException
        {
            final SunVMCommandLine cLine = new SunVMCommandLine();
            cLine.setAttribute(SunVMCommandLine.TRANSFORMATION_AGENT_JAR, "");
            cLine.setAttribute(SunVMCommandLine.DEBUGMODE, SunVMCommandLine.FALSE);
            cLine.setMainClass(ProcessServerConstants.PROC_SERVER_MAINCLASS);
            cLine.setAttribute(SunVMCommandLine.JVM_LOCATION, TestUtils.getProperty(SunVMCommandLine.JVM_LOCATION));
            cLine.addClasspathElement(TestUtils.getProperty(TestUtils.PROCSERVER_CLASS_PATH));
            cLine.addClasspathElement(TestUtils.getProperty(TestUtils.LOG4J_JAR_PATH));
            PyExecCommandLine pecl = new PyExecCommandLine(cLine);
            
            pecl.setAttribute(GLOBAL_AGENT_ADDRESS,
                    TestUtils.getProperty(GLOBAL_AGENT_ADDRESS));
            pecl.setAttribute(CALLBACK_OBJECT_PATH,
                    TestUtils.getProperty(CALLBACK_OBJECT_PATH));
            pecl.setAttribute(RMI_REGISTRY_PORT,
                    TestUtils.getProperty(RMI_REGISTRY_PORT));
            pecl.setAttribute(
                    LOG4JCONFIG,
                    log4jconfurl);
            pecl.setAttribute(TRANSPORT_PROTOCOL, TCP);
            pecl.setAttribute(LR_INSTANTIATION_POLICY, SHOULD_START_NEW);
            return pecl;
        }
	}
	
	private class RemoteProcessSpec implements IConfigurable{
		
		private Map<String, String> atts = Collections.synchronizedMap(new HashMap<String, String>());

		public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException {
            if(!atts.containsKey(key))fail();
			return atts.get(key);
		}

		public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException {
			atts.put(key, val);
		}

		public boolean isWritable() { return true; }
		
	}
}
