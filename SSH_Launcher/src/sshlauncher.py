""" 
  
  This module contains a class that acts as a shell frontend to 
  sshexecutor. 

  This software is licensed under the EPL (Eclipse Public License) V 1.0
  See http://www.eclipse.org/org/documents/epl-v10.html for details.
  
"""
import sys
import getopt
import sshexecutor

def main(argv):
    try:
        opts, args = getopt.getopt(argv, "h:u:P:p:", ["host=", "user=", "password=", "port="])
    except getopt.error, msg:
        print >>sys.stderr, msg
        usage()
        return 2

    # Command line options.
    host = None
    user = None
    password = None
    port = None

    for opt, arg in opts:
        if opt in ("-h", "--host"):
            host = arg
        elif opt in("-u", "--user"):
            user = arg
        elif opt in("-P","--password"):
            password = arg 
        elif opt in("-p","--port"):
            port = arg
    
    # Checks if all options have been entered.
    if host == None or user == None or password == None or port == None:
        usage()
        return 2
    
    # Lanches an executor and calls the remote command.
    executor = sshexecutor.SSHExecutor(user, host, password, port);
    return executor.performRemoteCommand(args);
    
def usage():
    print >>sys.stderr, "Required parameters missing. Usage:"
    print >>sys.stderr, sys.argv[0], "-u <login> -P <password> -h <host> -p <port>" 

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
    
