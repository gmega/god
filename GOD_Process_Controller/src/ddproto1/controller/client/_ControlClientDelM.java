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

public final class _ControlClientDelM extends Ice._ObjectDelM implements _ControlClientDel
{
    public void
    notifyProcessDeath(int pHandle, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "notifyProcessDeath", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		__os.writeInt(pHandle);
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

    public void
    notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "notifyServerUp", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		ddproto1.controller.remote.ProcessServerPrxHelper.__write(__os, procServer);
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

    public void
    receiveStringFromSTDERR(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "receiveStringFromSTDERR", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		__os.writeInt(pHandle);
		__os.writeString(data);
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

    public void
    receiveStringFromSTDIN(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	IceInternal.Outgoing __og = __connection.getOutgoing(__reference, "receiveStringFromSTDIN", Ice.OperationMode.Normal, __ctx, __compress);
	try
	{
	    try
	    {
		IceInternal.BasicStream __os = __og.os();
		__os.writeInt(pHandle);
		__os.writeString(data);
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
