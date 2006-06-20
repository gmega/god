// **********************************************************************
//
// Copyright (c) 2003-2005 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.0.1

package ddproto1.controller.remote;

public interface _ProcessServerOperationsNC
{
    RemoteProcessPrx launch(ddproto1.controller.client.LaunchParameters parameters)
	throws ServerRequestException;

    java.util.LinkedList getProcessList();

    boolean isAlive();

    void shutdownServer(boolean shutdownChildProcesses);
}
