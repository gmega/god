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

public interface ProcessServerPrx extends Ice.ObjectPrx
{
    public RemoteProcessPrx launch(ddproto1.controller.client.LaunchParameters parameters)
	throws ServerRequestException;
    public RemoteProcessPrx launch(ddproto1.controller.client.LaunchParameters parameters, java.util.Map __ctx)
	throws ServerRequestException;

    public java.util.LinkedList getProcessList();
    public java.util.LinkedList getProcessList(java.util.Map __ctx);

    public boolean isAlive();
    public boolean isAlive(java.util.Map __ctx);

    public void shutdownServer(boolean shutdownChildProcesses);
    public void shutdownServer(boolean shutdownChildProcesses, java.util.Map __ctx);
}
