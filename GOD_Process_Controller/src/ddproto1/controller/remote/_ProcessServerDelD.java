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

public final class _ProcessServerDelD extends Ice._ObjectDelD implements _ProcessServerDel
{
    public java.util.LinkedList
    getProcessList(java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "getProcessList", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ProcessServer __servant = null;
		try
		{
		    __servant = (ProcessServer)__direct.servant();
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
		    return __servant.getProcessList(__current);
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
		ProcessServer __servant = null;
		try
		{
		    __servant = (ProcessServer)__direct.servant();
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

    public RemoteProcessPrx
    launch(ddproto1.controller.client.LaunchParameters parameters, java.util.Map __ctx)
	throws IceInternal.NonRepeatable,
	       ServerRequestException
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "launch", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ProcessServer __servant = null;
		try
		{
		    __servant = (ProcessServer)__direct.servant();
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
		    return __servant.launch(parameters, __current);
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
    shutdownServer(boolean shutdownChildProcesses, java.util.Map __ctx)
	throws IceInternal.NonRepeatable
    {
	Ice.Current __current = new Ice.Current();
	__initCurrent(__current, "shutdownServer", Ice.OperationMode.Normal, __ctx);
	while(true)
	{
	    IceInternal.Direct __direct = new IceInternal.Direct(__current);
	    try
	    {
		ProcessServer __servant = null;
		try
		{
		    __servant = (ProcessServer)__direct.servant();
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
		    __servant.shutdownServer(shutdownChildProcesses, __current);
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
