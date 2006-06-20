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

public interface ControlClientPrx extends Ice.ObjectPrx
{
    public void notifyProcessDeath(int pHandle);
    public void notifyProcessDeath(int pHandle, java.util.Map __ctx);

    public void notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle);
    public void notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle, java.util.Map __ctx);

    public void receiveStringFromSTDIN(int pHandle, String data);
    public void receiveStringFromSTDIN(int pHandle, String data, java.util.Map __ctx);

    public void receiveStringFromSTDIN_async(AMI_ControlClient_receiveStringFromSTDIN __cb, int pHandle, String data);
    public void receiveStringFromSTDIN_async(AMI_ControlClient_receiveStringFromSTDIN __cb, int pHandle, String data, java.util.Map __ctx);

    public void receiveStringFromSTDERR(int pHandle, String data);
    public void receiveStringFromSTDERR(int pHandle, String data, java.util.Map __ctx);

    public void receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data);
    public void receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data, java.util.Map __ctx);

    public void notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer);
    public void notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer, java.util.Map __ctx);
}
