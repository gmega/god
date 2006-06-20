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

public interface _ControlClientOperationsNC
{
    void notifyProcessDeath(int pHandle);

    void receiveStringFromSTDIN(int pHandle, String data);

    void receiveStringFromSTDERR(int pHandle, String data);

    void notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer);
}
