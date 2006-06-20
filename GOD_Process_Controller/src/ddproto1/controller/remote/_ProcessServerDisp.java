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

public abstract class _ProcessServerDisp extends Ice.ObjectImpl implements ProcessServer
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
	"::ddproto1::controller::remote::ProcessServer"
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

    public final java.util.LinkedList
    getProcessList()
    {
	return getProcessList(null);
    }

    public final boolean
    isAlive()
    {
	return isAlive(null);
    }

    public final RemoteProcessPrx
    launch(ddproto1.controller.client.LaunchParameters parameters)
	throws ServerRequestException
    {
	return launch(parameters, null);
    }

    public final void
    shutdownServer(boolean shutdownChildProcesses)
    {
	shutdownServer(shutdownChildProcesses, null);
    }

    public static IceInternal.DispatchStatus
    ___launch(ProcessServer __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	IceInternal.BasicStream __os = __inS.os();
	ddproto1.controller.client.LaunchParameters parameters;
	parameters = new ddproto1.controller.client.LaunchParameters();
	parameters.__read(__is);
	try
	{
	    RemoteProcessPrx __ret = __obj.launch(parameters, __current);
	    RemoteProcessPrxHelper.__write(__os, __ret);
	    return IceInternal.DispatchStatus.DispatchOK;
	}
	catch(ServerRequestException ex)
	{
	    __os.writeUserException(ex);
	    return IceInternal.DispatchStatus.DispatchUserException;
	}
    }

    public static IceInternal.DispatchStatus
    ___getProcessList(ProcessServer __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __os = __inS.os();
	java.util.LinkedList __ret = __obj.getProcessList(__current);
	ProcessListHelper.write(__os, __ret);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___isAlive(ProcessServer __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __os = __inS.os();
	boolean __ret = __obj.isAlive(__current);
	__os.writeBool(__ret);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___shutdownServer(ProcessServer __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	boolean shutdownChildProcesses;
	shutdownChildProcesses = __is.readBool();
	__obj.shutdownServer(shutdownChildProcesses, __current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    private final static String[] __all =
    {
	"getProcessList",
	"ice_id",
	"ice_ids",
	"ice_isA",
	"ice_ping",
	"isAlive",
	"launch",
	"shutdownServer"
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
		return ___getProcessList(this, in, __current);
	    }
	    case 1:
	    {
		return ___ice_id(this, in, __current);
	    }
	    case 2:
	    {
		return ___ice_ids(this, in, __current);
	    }
	    case 3:
	    {
		return ___ice_isA(this, in, __current);
	    }
	    case 4:
	    {
		return ___ice_ping(this, in, __current);
	    }
	    case 5:
	    {
		return ___isAlive(this, in, __current);
	    }
	    case 6:
	    {
		return ___launch(this, in, __current);
	    }
	    case 7:
	    {
		return ___shutdownServer(this, in, __current);
	    }
	}

	assert(false);
	return IceInternal.DispatchStatus.DispatchOperationNotExist;
    }
}
