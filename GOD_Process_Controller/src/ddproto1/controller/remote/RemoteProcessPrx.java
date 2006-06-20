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

public interface RemoteProcessPrx extends Ice.ObjectPrx
{
    public boolean isAlive();
    public boolean isAlive(java.util.Map __ctx);

    public void writeToSTDIN(String message)
	throws ServerRequestException;
    public void writeToSTDIN(String message, java.util.Map __ctx)
	throws ServerRequestException;

    public int getHandle();
    public int getHandle(java.util.Map __ctx);

    public void dispose();
    public void dispose(java.util.Map __ctx);
}
