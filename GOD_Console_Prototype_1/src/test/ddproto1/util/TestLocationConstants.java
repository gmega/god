/*
 * Created on 31/07/2006
 * 
 * file: TestLocationConstants.java
 */
package ddproto1.util;

public interface TestLocationConstants {
    /** Location constants for DistributedThreadTest */
    public static final String DT_TEST_SERVER_BKP_CL = "implementation.QuoterImpl";
    public static final int DT_TEST_FINAL_BKP_LINE = 94;
    public static final int DT_TEST_SPIN_BKP_LINE = 61;
    public static final int DT_TEST_CHAINED_UPCALL_LINE = 88;

    public static final String DT_TEST_CLIENT_BKP_CL = "Client";
    public static final int DT_TEST_CLIENT_BKP_LINE = 85;
    public static final String DT_TEST_STEP_INTO_THREAD_NAME = "ACME";
    
    public static final int DT_TEST_FIRST_QUOTERIMPL_EXECUTABLE_LINE = 57;
    public static final int DT_TEST_CLIENT_EXCEPTIONHANDLER_LINE = 89;
    
    /** Location constants for CondensedSingleNodeTest */
    public static final String CSN_TEST_LOOP_BKP_CL = "SimpleClass";
    public static final int CSN_TEST_BKP_LINE = 37;
    public static final int CSN_TEST_STEPINTO_LINE = 43;

}
