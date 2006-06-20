// **********************************************************************
//
// Copyright (c) 2003-2005 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.0.1

package ddproto1.controller.client;

public interface _ControlClientDel extends Ice._ObjectDel
{
    void notifyProcessDeath(int pHandle, java.util.Map __ctx)
	throws IceInternal.NonRepeatable;

    void receiveStringFromSTDIN(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable;

    void receiveStringFromSTDERR(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable;

    void notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer, java.util.Map __ctx)
	throws IceInternal.NonRepeatable;
}
