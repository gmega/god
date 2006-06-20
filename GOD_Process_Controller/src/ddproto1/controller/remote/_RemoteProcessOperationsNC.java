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

public interface _RemoteProcessOperationsNC
{
    boolean isAlive();

    void writeToStdout(String message)
	throws ServerRequestException;

    int getHandle();

    void dispose();
}
