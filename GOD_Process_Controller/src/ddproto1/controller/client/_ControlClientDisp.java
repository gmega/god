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

public abstract class _ControlClientDisp extends Ice.ObjectImpl implements ControlClient
{
    protected void
    ice_copyStateFrom(Ice.Object __obj)
	throws java.lang.CloneNotSupportedException
    {
	throw new java.lang.CloneNotSupportedException();
    }

    public static final String[] __ids =
    {
	"::Ice::Object",
	"::ddproto1::controller::client::ControlClient"
    };

    public boolean
    ice_isA(String s)
    {
	return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public boolean
    ice_isA(String s, Ice.Current __current)
    {
	return java.util.Arrays.binarySearch(__ids, s) >= 0;
    }

    public String[]
    ice_ids()
    {
	return __ids;
    }

    public String[]
    ice_ids(Ice.Current __current)
    {
	return __ids;
    }

    public String
    ice_id()
    {
	return __ids[1];
    }

    public String
    ice_id(Ice.Current __current)
    {
	return __ids[1];
    }

    public static String
    ice_staticId()
    {
	return __ids[1];
    }

    public final void
    notifyProcessDeath(int pHandle)
    {
	notifyProcessDeath(pHandle, null);
    }

    public final void
    notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer)
    {
	notifyServerUp(procServer, null);
    }

    public final void
    receiveStringFromSTDERR(int pHandle, String data)
    {
	receiveStringFromSTDERR(pHandle, data, null);
    }

    public final void
    receiveStringFromSTDOUT(int pHandle, String data)
    {
	receiveStringFromSTDOUT(pHandle, data, null);
    }

    public static IceInternal.DispatchStatus
    ___notifyProcessDeath(ControlClient __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	int pHandle;
	pHandle = __is.readInt();
	__obj.notifyProcessDeath(pHandle, __current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___receiveStringFromSTDOUT(ControlClient __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	int pHandle;
	pHandle = __is.readInt();
	String data;
	data = __is.readString();
	__obj.receiveStringFromSTDOUT(pHandle, data, __current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___receiveStringFromSTDERR(ControlClient __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	int pHandle;
	pHandle = __is.readInt();
	String data;
	data = __is.readString();
	__obj.receiveStringFromSTDERR(pHandle, data, __current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___notifyServerUp(ControlClient __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	ddproto1.controller.remote.ProcessServerPrx procServer;
	procServer = ddproto1.controller.remote.ProcessServerPrxHelper.__read(__is);
	__obj.notifyServerUp(procServer, __current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    private final static String[] __all =
    {
	"ice_id",
	"ice_ids",
	"ice_isA",
	"ice_ping",
	"notifyProcessDeath",
	"notifyServerUp",
	"receiveStringFromSTDERR",
	"receiveStringFromSTDOUT"
    };

    public IceInternal.DispatchStatus
    __dispatch(IceInternal.Incoming in, Ice.Current __current)
    {
	int pos = java.util.Arrays.binarySearch(__all, __current.operation);
	if(pos < 0)
	{
	    return IceInternal.DispatchStatus.DispatchOperationNotExist;
	}

	switch(pos)
	{
	    case 0:
	    {
		return ___ice_id(this, in, __current);
	    }
	    case 1:
	    {
		return ___ice_ids(this, in, __current);
	    }
	    case 2:
	    {
		return ___ice_isA(this, in, __current);
	    }
	    case 3:
	    {
		return ___ice_ping(this, in, __current);
	    }
	    case 4:
	    {
		return ___notifyProcessDeath(this, in, __current);
	    }
	    case 5:
	    {
		return ___notifyServerUp(this, in, __current);
	    }
	    case 6:
	    {
		return ___receiveStringFromSTDERR(this, in, __current);
	    }
	    case 7:
	    {
		return ___receiveStringFromSTDOUT(this, in, __current);
	    }
	}

	assert(false);
	return IceInternal.DispatchStatus.DispatchOperationNotExist;
    }
}
