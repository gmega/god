/*
 * Created on Sep 9, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: ProtocolConstants.java
 */

package ddproto1.commons;

/**
 * @author giuliano
 *
 */
public interface DebuggerConstants {
    public static final int LUID_MASK = 16777215;
    public static final int GID_MASK = -16777216;
    
    public static final byte GID_BYTES = 1;
    public static final byte SIZE_BYTES = 3;
    public static final byte THREAD_LUID_BYTES = 3;
    public static final byte STATUS_FIELD_OFFSET = 0;
    
    public static final byte ICW_INVALID_GID = 1;
    public static final byte ICW_ILLEGAL_ATTRIBUTE = 2;
    
    public static final byte UNKNOWN = -1;
    public static final byte OK = 0;
    public static final byte NO_HANDLER_ERR = 1;
    public static final byte EMPTY_REQUEST_ERR = 2;
    public static final byte PROTOCOL_ERR = 3;
    public static final byte HANDLER_FAILURE_ERR = 4;
    public static final byte MAX_CONNECTIONS_REACHED_ERR = 5;
    
    public static final byte START_REQUEST = -1;
    public static final byte REQUEST = -2;
    public static final byte END_REQUEST = -3;
    public static final byte ECHO_REQUEST = -4;
    public static final byte NOTIFICATION = -5;
    
    public static final byte STEPPING_INTO = 1;
    public static final byte STEPPING_OVER = 2;
    public static final byte RUNNING = 4;
    public static final byte SUSPENDED = 8;
    public static final byte STEPPING_REMOTE = 16;
    
    public static final byte EVENT_TYPE_IDX = 0;
    public static final byte CLIENT_DOWNCALL = 1;
    public static final byte CLIENT_UPCALL = 2;
    public static final byte SERVER_RECEIVE = 3;
    public static final byte SERVER_RETURN = 4;
    public static final byte SIGNAL_BOUNDARY = 5;
    public static final byte UNKNOWN_EVENT_TYPE_ERR = 6;
    
    public static final String RUNNABLE_INTF_NAME = "java.lang.Runnable";
    public static final String RUN_METHOD_NAME = "run";
    
    public static final String TAGGER_REG_METHOD_NAME = "ddproto1.localagent.Tagger.haltForRegistration";
    public static final String THREAD_TRAP_METHOD_NAME = "ddproto1.localagent.Tagger.stepMeOut";
    public static final String TAGGER_CLASS_NAME = "ddproto1.localagent.Tagger";
    public static final String INSTANCE_GETTER = "getInstance";
    public static final String UUID_GETTER = "currentTag";
    public static final String UUID_SETTER = "assignCurrent";
    public static final String STEP_GETTER = "isStepping";
    
    public static final String VMM_KEY = "vmm";
    
}
