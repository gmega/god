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

public abstract class _RemoteProcessDisp extends Ice.ObjectImpl implements RemoteProcess
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
	"::ddproto1::controller::remote::RemoteProcess"
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
    dispose()
    {
	dispose(null);
    }

    public final int
    getHandle()
    {
	return getHandle(null);
    }

    public final boolean
    isAlive()
    {
	return isAlive(null);
    }

    public final void
    writeToStdout(String message)
	throws ServerRequestException
    {
	writeToStdout(message, null);
    }

    public static IceInternal.DispatchStatus
    ___isAlive(RemoteProcess __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __os = __inS.os();
	boolean __ret = __obj.isAlive(__current);
	__os.writeBool(__ret);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___writeToStdout(RemoteProcess __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __is = __inS.is();
	IceInternal.BasicStream __os = __inS.os();
	String message;
	message = __is.readString();
	try
	{
	    __obj.writeToStdout(message, __current);
	    return IceInternal.DispatchStatus.DispatchOK;
	}
	catch(ServerRequestException ex)
	{
	    __os.writeUserException(ex);
	    return IceInternal.DispatchStatus.DispatchUserException;
	}
    }

    public static IceInternal.DispatchStatus
    ___getHandle(RemoteProcess __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	IceInternal.BasicStream __os = __inS.os();
	int __ret = __obj.getHandle(__current);
	__os.writeInt(__ret);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    public static IceInternal.DispatchStatus
    ___dispose(RemoteProcess __obj, IceInternal.Incoming __inS, Ice.Current __current)
    {
	__checkMode(Ice.OperationMode.Normal, __current.mode);
	__obj.dispose(__current);
	return IceInternal.DispatchStatus.DispatchOK;
    }

    private final static String[] __all =
    {
	"dispose",
	"getHandle",
	"ice_id",
	"ice_ids",
	"ice_isA",
	"ice_ping",
	"isAlive",
	"writeToStdout"
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
		return ___dispose(this, in, __current);
	    }
	    case 1:
	    {
		return ___getHandle(this, in, __current);
	    }
	    case 2:
	    {
		return ___ice_id(this, in, __current);
	    }
	    case 3:
	    {
		return ___ice_ids(this, in, __current);
	    }
	    case 4:
	    {
		return ___ice_isA(this, in, __current);
	    }
	    case 5:
	    {
		return ___ice_ping(this, in, __current);
	    }
	    case 6:
	    {
		return ___isAlive(this, in, __current);
	    }
	    case 7:
	    {
		return ___writeToStdout(this, in, __current);
	    }
	}

	assert(false);
	return IceInternal.DispatchStatus.DispatchOperationNotExist;
    }
}
