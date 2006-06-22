/*
 * Created on Jun 21, 2006
 * 
 * file: LaunchParametersDTO.java
 */
package ddproto1.controller.interfaces;

import java.io.Serializable;

/**
 * Non-thread-safe.
 * 
 * @author giuliano
 */
public class LaunchParametersDTO implements Serializable{
    
    private static final long serialVersionUID = -2024763738535455050L;
    
    /** Please check the getter javadocs for the meaning of each of these
     * variables.
     */
    private String [] commandLine;
    
    private EnvironmentVariable [] envVars;
    
    private int pollInterval;

    int maxUnflushedSize;
    
    int flushTimeout;
    
    /**
     * Constructor for LaunchParametersDTO. Please check the 
     * getters of each parameter to understand their meanings. 
     * 
     * @param commandLine @see getCommandLine()
     * @param envVars @see getEnvVars()
     * @param pollInterval @see getPollInterval()
     * @param maxUnflushedSize @see getMaxUnflushedSize()
     * @param flushTimeout @see getFlushTimeout()
     */
    public LaunchParametersDTO(String [] commandLine,
            EnvironmentVariable [] envVars, int pollInterval,
            int maxUnflushedSize, int flushTimeout){
        
        this.commandLine = commandLine.clone();
        this.envVars = envVars.clone();
        this.pollInterval = pollInterval;
        this.maxUnflushedSize = maxUnflushedSize;
        this.flushTimeout = flushTimeout;
    }

    /**
     * Sequence of strings that contain the command line
     * to be executed at the remote machine.
     * 
     * @return Returns the commandLine array.
     */
    public String[] getCommandLine() {
        return commandLine;
    }

    /**
     * Sequence of environment variable descriptions that 
     * must be set at the remote machine *before* the application
     * is launched.
     *
     * @return Returns the envVars.
     */
    public EnvironmentVariable[] getEnvVars() {
        return envVars;
    }

    /**
     * Defines the polling interval for determining whether the 
     * application is dead or not. You should set this to -1 to
     * use the default value.
     *
     * @return Returns the flushTimeout.
     */
    public int getPollInterval() {
        return flushTimeout;
    }

    /**
     * Defines an upper bound on the unflushed buffer for the 
     * application's output streams (STDOUT and STDERR). After
     * maxUnflushedSize characters are produced, the buffer will
     * be flushed and receiveStringFrom notification will be 
     * emmited.
     * 
     * @return Returns the maxUnflushedSize.
     */
    public int getMaxUnflushedSize() {
        return maxUnflushedSize;
    }

    /**
     * Defines a timeout for flushing the remote application's
     * output streams buffer. This ensures that no character will
     * be stuck at server-side buffers because maxUnflushedSize 
     * hasn't been reached within a reasonable ammount of time.
     *   
     * @return Returns the pollInterval.
     */
    public int getFlushTimeout() {
        return pollInterval;
    }
    
    
    public static class EnvironmentVariable implements Serializable{
        
        private static final long serialVersionUID = 7528855527495648901L;
        
        private String key;
        private String value;
        
        public EnvironmentVariable(String key, String value){
            this.key = key;
            this.value = value;
        }
        
        protected String getKey() { return key; }
        protected String getValue() { return value; }
    }

}
