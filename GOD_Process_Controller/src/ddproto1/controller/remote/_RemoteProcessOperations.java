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

public interface _RemoteProcessOperations
{
    boolean isAlive(Ice.Current __current);

    void writeToStdout(String message, Ice.Current __current)
	throws ServerRequestException;

    int getHandle(Ice.Current __current);

    void dispose(Ice.Current __current);
}
