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

public final class _RemoteProcessDelD extends Ice._ObjectDelD implements _RemoteProcessDel
{
    public void
    dispose(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "dispose", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		RemoteProcess __servant = null;
		try
		{
		    __servant = (RemoteProcess)__direct.servant();
		}
		catch(ClassCastException __ex)
		{
		    Ice.OperationNotExistException __opEx = new Ice.OperationNotExistException();
		    __opEx.id = __current.id;
		    __opEx.facet = __current.facet;
		    __opEx.operation = __current.operation;
		    throw __opEx;
		}
		try
		{
		    __servant.dispose(__current);
		    return;
		}
		catch(Ice.LocalException __ex)
		{
		    throw new IceInternal.NonRepeatable(__ex);
		}
	    }
	    finally
	    {
		__direct.destroy();
	    }
	}
    }

    public int
    getHandle(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "getHandle", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		RemoteProcess __servant = null;
		try
		{
		    __servant = (RemoteProcess)__direct.servant();
		}
		catch(ClassCastException __ex)
		{
		    Ice.OperationNotExistException __opEx = new Ice.OperationNotExistException();
		    __opEx.id = __current.id;
		    __opEx.facet = __current.facet;
		    __opEx.operation = __current.operation;
		    throw __opEx;
		}
		try
		{
		    return __servant.getHandle(__current);
		}
		catch(Ice.LocalException __ex)
		{
		    throw new IceInternal.NonRepeatable(__ex);
		}
	    }
	    finally
	    {
		__direct.destroy();
	    }
	}
    }

    public boolean
    isAlive(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "isAlive", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		RemoteProcess __servant = null;
		try
		{
		    __servant = (RemoteProcess)__direct.servant();
		}
		catch(ClassCastException __ex)
		{
		    Ice.OperationNotExistException __opEx = new Ice.OperationNotExistException();
		    __opEx.id = __current.id;
		    __opEx.facet = __current.facet;
		    __opEx.operation = __current.operation;
		    throw __opEx;
		}
		try
		{
		    return __servant.isAlive(__current);
		}
		catch(Ice.LocalException __ex)
		{
		    throw new IceInternal.NonRepeatable(__ex);
		}
	    }
	    finally
	    {
		__direct.destroy();
	    }
	}
    }

    public void
    writeToSTDIN(String message, java.util.Map __ctx)
	throws IceInternal.NonRepeatable,
	       ServerRequestException
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "writeToSTDIN", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		RemoteProcess __servant = null;
		try
		{
		    __servant = (RemoteProcess)__direct.servant();
		}
		catch(ClassCastException __ex)
		{
		    Ice.OperationNotExistException __opEx = new Ice.OperationNotExistException();
		    __opEx.id = __current.id;
		    __opEx.facet = __current.facet;
		    __opEx.operation = __current.operation;
		    throw __opEx;
		}
		try
		{
		    __servant.writeToSTDIN(message, __current);
		    return;
		}
		catch(Ice.LocalException __ex)
		{
		    throw new IceInternal.NonRepeatable(__ex);
		}
	    }
	    finally
	    {
		__direct.destroy();
	    }
	}
    }
}
