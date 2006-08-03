/*
 * Created on 27/07/2006
 * 
 * file: TestConfigurationConstants.java
 */
package ddproto1.util;

import ddproto1.launcher.IJVMCommandLine;
import ddproto1.launcher.procserver.PyExpectSSHExecutor;
import ddproto1.launcher.procserver.SunVMCommandLine;

public interface TestConfigurationConstants {
    public static String [] addresses = {
        "machine-address1", "machine-address2", "machine-address3"
    };
    
    public static final String MY_ADDRESS = "global-agent-address";
    public static final String PROCSERVER_CLASS_PATH = "procserver-class-path";
    public static final String LOG4J_JAR_PATH = "log4j-jar-path";
    public static final String DDUSER_NAME = PyExpectSSHExecutor.USER;
    public static final String DDUSER_PASS = PyExpectSSHExecutor.PASSWORD;
    public static final String SSH_SERVER_PORT = PyExpectSSHExecutor.SSH_PORT;
    public static final String EXPECT_SCRIPT = PyExpectSSHExecutor.PEXPECT_SCRIPT;
    public static final String PY_INTERPRETER = PyExpectSSHExecutor.PYTHON_INTERPRETER;
    public static final String JVM_LOCATION = IJVMCommandLine.JVM_LOCATION;
    public static final String PROJECT_BINARIES = "main-plugin-runtime";
    public static final String TEST_RESOURCES = "test-resources-dir";
    
    public static final String BASEDIR_URL = "basedir-url";
    public static final String BASEDIR = "basedir";
    public static final String TESTS_DIR = "tests-dir";
    public static final String MAIN_DIR = "main-dir";
    public static final String CONFIG_DIR = "config-dir";
    public static final String SPECS_DIR = "specs-dir";
    public static final String RESOURCES_DIR = "resources-dir";
    
    public static final String COMP_TOC_DIR = "component-tests-toc";
    public static final String COMP_SPECS_DIR = "component-tests-specs";
    public static final String COMP_CONF_FILE = "component-tests-configfile";
    
    public static final String IOCTEST_CONF_FILE = "ioc-test-configfile";
    
    public static final String TEST_TEXT_URL = "test-text-url";
    
    public static final String TEST_PROPS_URL = "test.properties.url";
    public static final String BSTRAP_PROPS_URL = "bootstrap.properties.url";
    public static final String ABS_PROPS_URL = "absolute.properties.url";
    
    public static final String DDTEST_CONF_FILE="dthread-test-configfile";
    
    public static final String CLIENT_PROJECT_NAME = "client-project-name";
    public static final String SERVER_PROJECT_NAME = "server-project-name";
    public static final String MISC_PROJECT_NAME = "misc-project-name";
    
    public static final String JACORB_HOME = "jacorb-home";
    public static final String JACORB_FULL_LIBS = "jacorb-full-libs";
    
    public static final String WORKSPACE_HOME = "workspace-home";
    public static final String TEST_PROJECTS_HOME = "test-projects-home";
}
