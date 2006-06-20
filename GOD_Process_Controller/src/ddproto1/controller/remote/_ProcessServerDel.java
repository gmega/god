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

public interface _ProcessServerDel extends Ice._ObjectDel
{
    RemoteProcessPrx launch(ddproto1.controller.client.LaunchParameters parameters, java.util.Map __ctx)
	throws IceInternal.NonRepeatable,
	       ServerRequestException;

    java.util.LinkedList getProcessList(java.util.Map __ctx)
	throws IceInternal.NonRepeatable;

    boolean isAlive(java.util.Map __ctx)
	throws IceInternal.NonRepeatable;

    void shutdownServer(boolean shutdownChildProcesses, java.util.Map __ctx)
	throws IceInternal.NonRepeatable;
}
