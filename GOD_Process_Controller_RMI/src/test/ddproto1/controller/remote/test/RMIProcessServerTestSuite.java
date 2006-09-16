/*
 * Created on 16/09/2006
 * 
 * file: RMIProcessServerTestSuite.java
 */
package ddproto1.controller.remote.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RMIProcessServerTestSuite extends TestSuite {
    public static Test suite(){
        return new RMIProcessServerTestSuite();
    }
    
    public RMIProcessServerTestSuite(){
        /** Base tasks */
        addTest(new TestSuite(ConfigurationDecorator.class));

        /** Actual tests. */
        addTest(new TestSuite(ProcessPollTaskTest.class));
        addTest(new TestSuite(RMIMultiDeathTest.class));
    }

}
