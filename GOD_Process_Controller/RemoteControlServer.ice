#ifndef __DD_CONTROL_SERVER__
#define __DD_CONTROL_SERVER__

module ddproto1{
	module controller{
	
		/** Forward declaration of interface ProcessServer */
		module remote {
			interface ProcessServer;
		};
		
		/***************************************************************
		 *                                                             *
		 * Module client contains all interfaces and objects that are  *
		 * provided by the client (the process that wishes to manage   *
		 * remote processes).                                          *
		 *                                                             *
		 ***************************************************************/
		module client {
			sequence <string> ParameterArray;
				
			/**
			 * This interface represents the notification interface for the
			 * control client. The control client is the entity that places
			 * requests through the ProcessServer interface. 
			 *
			 * This interface is adequate for one-to-one relationships. It
			 * assumes that the client will register one object for interacting
			 * with each process server.
			 */	
			interface ControlClient{
				/**
				 * Notifies the client that the process of handle pHandle
				 * has died.
				 */
				["ami"] void notifyProcessDeath(int pHandle);
				
				/**
				 * Notifies the client that a new segment of characters has
				 * been sent to the standard output of the remote application.
				 */
				["ami"] void receiveStringFromSTDOUT(int pHandle, string data);
				
				/**
				 * Notifies the client that a new segment of characters has
				 * been sent to the standard error output of the remote 
				 * application.
				 */
				["ami"] void receiveStringFromSTDERR(int pHandle, string data); 
				
				/**
				 * Notifies the client that this process server is up 
				 * and running. Useful for synchronization if the client 
				 * is the one launching the server.
				 */
				void notifyServerUp(ddproto1::controller::remote::ProcessServer* procServer);
			};
			
			/**
			 * Struct for conveying environment variables.
			 */
			struct EnvironmentVariable{
				string key;
				string value;
			};
			
			["java:type:java.util.ArrayList"] sequence <EnvironmentVariable> EnvironmentVariables;
			
			/**
			 * This structure aggregates all data that is required to launch a 
			 * remote process.
			 */
			struct LaunchParameters{
			    /**
			     * Sequence of strings that contain the command line
			     * to be executed at the remote machine.
			     */
				ParameterArray commandLine;
				
				/**
				 * Sequence of environment variable descriptions that 
				 * must be set at the remote machine *before* the application
				 * is launched.
				 */
				EnvironmentVariables envVars;
				
				/**
				 * Defines the polling interval for determining whether the 
				 * application is dead or not. You should set this to -1 to
				 * use the default value.
				 */
				int pollInterval;
				
				/**
				 * Defines an upper bound on the unflushed buffer for the 
				 * application's output streams (STDOUT and STDERR). After
				 * maxUnflushedSize characters are produced, the buffer will
				 * be flushed and receiveStringFrom* notification will be 
				 * emmited.
				 */
				int maxUnflushedSize;
				
				/**
				 * Defines a timeout for flushing the remote application's
				 * output streams buffer. This ensures that no character will
				 * be stuck at server-side buffers because maxUnflushedSize 
				 * hasn't been reached within a reasonable ammount of time.
				 */
				int flushTimeout;
			};
		};
	
     	/***************************************************************
		 *                                                             *
		 * Module remote, in turn, defines the remote management       *
		 * interfaces that will be exported by the process server.     *
		 *                                                             *
		 ***************************************************************/
		module remote{
		
			exception ServerRequestException{
				string reason;
				int errorCode;
			};
		
			/**
			 * This interface provides control over remote processes. 
			 */
			interface RemoteProcess{
			    /**
			     * Returns true if the remote process has terminated, and
			     * false otherwise.
			     */
				bool isAlive();
				/**
				 * Writes a sequence of characters to the standard input
				 * of the remote process. Flush is immediate.
				 *
				 * @throws ServerRequestException if an I/O exception occurs
				 * at the server side.
				 */
				void writeToSTDIN(string message) throws ServerRequestException;
				
				/**
				 * Returns the numeric handle of this remote process. Numeric
				 * handles are unique in the scope of a single ProcessServer.
				 */
				int getHandle();
				
				/**
				 * Disposes of this proxy. This will cause the remote 
				 * process to be (forcefully) terminated. Subsequent calls to 
				 * ProcessServer::getProcessList() will not return this
				 * proxy as well. All calls dispatched to this proxy will
				 * likely result in exceptions after dispose is called. 
				 */
				void dispose();
			};
		
			["java:type:java.util.LinkedList"] sequence <RemoteProcess*> ProcessList;
		
			/**
			 * This class represents the remote process server. It'll allow the user
			 * to launch and kill remote processes, as well as obtain the list of 
			 * active processes.
			 *
			 */
			interface ProcessServer{
			    /** 
			     * Launches a remote process with the given parameters.
			     *
			     * @throws ServerRequestException if launch fails.
			     */
				RemoteProcess* launch(ddproto1::controller::client::LaunchParameters parameters)
					throws ServerRequestException;
				
			   /** 
			    * Retrieves a list of registered processes, dead or not. A RemoteProcess instance
			    * gets out of this list only after RemoteProcess.dispose() is called. 
			    */
				ProcessList getProcessList();
				
				/**
				 * Keep alive method for the process server.
				 */
				bool isAlive();
				
				/**
				 * Shuts down the remote process server. If 'true' is passed on as a
				 * parameter, all controlled processes will be killed as well. Otherwise
				 * they'll be left running.
				 */
				void shutdownServer(bool shutdownChildProcesses);
			};
		};
	};
};

#endif