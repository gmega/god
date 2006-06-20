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

public final class _ProcessServerDelM extends Ice._ObjectDelM implements _ProcessServerDel
{
    public java.util.LinkedList
    getProcessList(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "getProcessList", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    boolean __ok = __og.invoke();
	    try
	    {
		IceInternal.BasicStream __is = __og.is();
		if(!__ok)
		{
		    try
		    {
			__is.throwException();
		    }
		    catch(Ice.UserException __ex)
		    {
			throw new Ice.UnknownUserException(__ex.ice_name());
		    }
		}
		java.util.LinkedList __ret;
		__ret = ProcessListHelper.read(__is);
		return __ret;
	    }
	    catch(Ice.LocalException __ex)
	    {
		throw new IceInternal.NonRepeatable(__ex);
	    }
	}
	finally
	{
	    __connection.reclaimOutgoing(__og);
	}
    }

    public boolean
    isAlive(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "isAlive", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    boolean __ok = __og.invoke();
	    try
	    {
		IceInternal.BasicStream __is = __og.is();
		if(!__ok)
		{
		    try
		    {
			__is.throwException();
		    }
		    catch(Ice.UserException __ex)
		    {
			throw new Ice.UnknownUserException(__ex.ice_name());
		    }
		}
		boolean __ret;
		__ret = __is.readBool();
		return __ret;
	    }
	    catch(Ice.LocalException __ex)
	    {
		throw new IceInternal.NonRepeatable(__ex);
	    }
	}
	finally
	{
	    __connection.reclaimOutgoing(__og);
	}
    }

    public RemoteProcessPrx
    launch(ddproto1.controller.client.LaunchParameters parameters, java.util.Map __ctx)
	throws IceInternal.NonRepeatable,
	       ServerRequestException
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "launch", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		parameters.__write(__os);
	    }
	    catch(Ice.LocalException __ex)
	    {
		__og.abort(__ex);
	    }
	    boolean __ok = __og.invoke();
	    try
	    {
		IceInternal.BasicStream __is = __og.is();
		if(!__ok)
		{
		    try
		    {
			__is.throwException();
		    }
		    catch(ServerRequestException __ex)
		    {
			throw __ex;
		    }
		    catch(Ice.UserException __ex)
		    {
			throw new Ice.UnknownUserException(__ex.ice_name());
		    }
		}
		RemoteProcessPrx __ret;
		__ret = RemoteProcessPrxHelper.__read(__is);
		return __ret;
	    }
	    catch(Ice.LocalException __ex)
	    {
		throw new IceInternal.NonRepeatable(__ex);
	    }
	}
	finally
	{
	    __connection.reclaimOutgoing(__og);
	}
    }

    public void
    shutdownServer(boolean shutdownChildProcesses, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "shutdownServer", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		__os.writeBool(shutdownChildProcesses);
	    }
	    catch(Ice.LocalException __ex)
	    {
		__og.abort(__ex);
	    }
	    boolean __ok = __og.invoke();
	    try
	    {
		IceInternal.BasicStream __is = __og.is();
		if(!__ok)
		{
		    try
		    {
			__is.throwException();
		    }
		    catch(Ice.UserException __ex)
		    {
			throw new Ice.UnknownUserException(__ex.ice_name());
		    }
		}
	    }
	    catch(Ice.LocalException __ex)
	    {
		throw new IceInternal.NonRepeatable(__ex);
	    }
	}
	finally
	{
	    __connection.reclaimOutgoing(__og);
	}
    }
}
