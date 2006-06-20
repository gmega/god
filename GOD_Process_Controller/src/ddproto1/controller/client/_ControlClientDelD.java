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

public final class _ControlClientDelD extends Ice._ObjectDelD implements _ControlClientDel
{
    public void
    notifyProcessDeath(int pHandle, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "notifyProcessDeath", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ControlClient __servant = null;
		try
		{
		    __servant = (ControlClient)__direct.servant();
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
		    __servant.notifyProcessDeath(pHandle, __current);
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

    public void
    notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "notifyServerUp", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ControlClient __servant = null;
		try
		{
		    __servant = (ControlClient)__direct.servant();
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
		    __servant.notifyServerUp(procServer, __current);
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

    public void
    receiveStringFromSTDERR(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "receiveStringFromSTDERR", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ControlClient __servant = null;
		try
		{
		    __servant = (ControlClient)__direct.servant();
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
		    __servant.receiveStringFromSTDERR(pHandle, data, __current);
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

    public void
    receiveStringFromSTDIN(int pHandle, String data, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "receiveStringFromSTDIN", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ControlClient __servant = null;
		try
		{
		    __servant = (ControlClient)__direct.servant();
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
		    __servant.receiveStringFromSTDIN(pHandle, data, __current);
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
