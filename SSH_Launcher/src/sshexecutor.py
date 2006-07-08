"""
  
  This module contains the ssh executor class, which uses Python Expect
  to launch a remote command.

  This software is licensed under the EPL (Eclipse Public License) V 1.0
  See http://www.eclipse.org/org/documents/epl-v10.html for details.
  
"""

DEFAULT_EOF_TO_DEATH_TIMEOUT = 4
DEFAULT_DEATH_POLLING_INTERVAL = 0.5
DEFAULT_SSH_ANSWER_TIMEOUT = 20
DEFAULT_SSH_PORT = 22

import pexpect
import time

class SSHExecutor:
    
    def __init__(self, user, host, password, 
                 port=DEFAULT_SSH_PORT, 
                 ansTimeout=DEFAULT_SSH_ANSWER_TIMEOUT):
        """
        Default constructor for class SSHExecutor. You should pass the remote login,
        password, and host address to it. You may optionally pass the ssh server 
        port (if it's different from the default) and a timeout value for interactions
        with the ssh client.
        """
        self.user = user
        self.host = host
        self.password = password
        self.port = port
        self.ansTimeout = ansTimeout
    
    def performRemoteCommand(self, commandList):
        
        baseParamList = ['-X', '-l', self.user, self.host, '-p', self.port]
        baseParamList.extend(commandList)
        
        child = pexpect.spawn ('ssh', baseParamList)
        
        # Loops infinitely
        while True:
            pat = child.expect(['assword', '\\(yes/no\\)\\?', 
                                'Connection to ' + self.host + ' closed', 
                                'not known', 
                                'Permission denied', 
                                pexpect.EOF, 
                                pexpect.TIMEOUT], 
                                timeout=self.ansTimeout)
            if pat == 0:
                child.sendline(self.password)
                continue
# Calling 'interact' will generate an exception if I launch the script from inside Eclipse or java.
#                try:
#                    child.interact()
#                except OSError:
                    # Interaction aborted, maybe program died?.
#                    if not self.__pollDeath(child, DEFAULT_EOF_TO_DEATH_TIMEOUT):
#                        # Program didn't die. We can't recover from this error.
#                        raise OSError
                    # Otherwise, continue.
#                    continue
            elif pat == 1:
                #I trust any server. Perhaps this could be configurable?
                child.sendline('yes') 
            else:
                self.__pollDeath(child)
                return child.exitstatus
                
                
                
    def __pollDeath(self, process, timeout=-1):
        startTime = time.time();
        
        while process.isalive():
            time.sleep(DEFAULT_DEATH_POLLING_INTERVAL)
            if (timeout != -1) and ((time.time() - startTime) > timeout):
                return False
            
        return True
    
    def __processCommandList(self, commandList):
        return ["\"%s\"" % element for element in commandList]
