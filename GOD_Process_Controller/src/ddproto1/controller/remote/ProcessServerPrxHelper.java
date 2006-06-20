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

public final class ProcessServerPrxHelper extends Ice.ObjectPrxHelperBase implements ProcessServerPrx
{
    public java.util.LinkedList
    getProcessList()
    {
	return getProcessList(__defaultContext());
    }

    public java.util.LinkedList
    getProcessList(java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		__checkTwowayOnly("getProcessList");
		Ice._ObjectDel __delBase = __getDelegate();
		_ProcessServerDel __del = (_ProcessServerDel)__delBase;
		return __del.getProcessList(__ctx);
	    }
	    catch(IceInternal.NonRepeatable __ex)
	    {
		__rethrowException(__ex.get());
	    }
	    catch(Ice.LocalException __ex)
	    {
		__cnt = __handleException(__ex, __cnt);
	    }
	}
    }

    public boolean
    isAlive()
    {
	return isAlive(__defaultContext());
    }

    public boolean
    isAlive(java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		__checkTwowayOnly("isAlive");
		Ice._ObjectDel __delBase = __getDelegate();
		_ProcessServerDel __del = (_ProcessServerDel)__delBase;
		return __del.isAlive(__ctx);
	    }
	    catch(IceInternal.NonRepeatable __ex)
	    {
		__rethrowException(__ex.get());
	    }
	    catch(Ice.LocalException __ex)
	    {
		__cnt = __handleException(__ex, __cnt);
	    }
	}
    }

    public RemoteProcessPrx
    launch(ddproto1.controller.client.LaunchParameters parameters)
	throws ServerRequestException
    {
	return launch(parameters, __defaultContext());
    }

    public RemoteProcessPrx
    launch(ddproto1.controller.client.LaunchParameters parameters, java.util.Map __ctx)
	throws ServerRequestException
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		__checkTwowayOnly("launch");
		Ice._ObjectDel __delBase = __getDelegate();
		_ProcessServerDel __del = (_ProcessServerDel)__delBase;
		return __del.launch(parameters, __ctx);
	    }
	    catch(IceInternal.NonRepeatable __ex)
	    {
		__rethrowException(__ex.get());
	    }
	    catch(Ice.LocalException __ex)
	    {
		__cnt = __handleException(__ex, __cnt);
	    }
	}
    }

    public void
    shutdownServer(boolean shutdownChildProcesses)
    {
	shutdownServer(shutdownChildProcesses, __defaultContext());
    }

    public void
    shutdownServer(boolean shutdownChildProcesses, java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_ProcessServerDel __del = (_ProcessServerDel)__delBase;
		__del.shutdownServer(shutdownChildProcesses, __ctx);
		return;
	    }
	    catch(IceInternal.NonRepeatable __ex)
	    {
		__rethrowException(__ex.get());
	    }
	    catch(Ice.LocalException __ex)
	    {
		__cnt = __handleException(__ex, __cnt);
	    }
	}
    }

    public static ProcessServerPrx
    checkedCast(Ice.ObjectPrx b)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (ProcessServerPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::remote::ProcessServer"))
		{
		    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static ProcessServerPrx
    checkedCast(Ice.ObjectPrx b, java.util.Map ctx)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (ProcessServerPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::remote::ProcessServer", ctx))
		{
		    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static ProcessServerPrx
    checkedCast(Ice.ObjectPrx b, String f)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::remote::ProcessServer"))
		{
		    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
		    h.__copyFrom(bb);
		    d = h;
		}
	    }
	    catch(Ice.FacetNotExistException ex)
	    {
	    }
	}
	return d;
    }

    public static ProcessServerPrx
    checkedCast(Ice.ObjectPrx b, String f, java.util.Map ctx)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::remote::ProcessServer", ctx))
		{
		    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
		    h.__copyFrom(bb);
		    d = h;
		}
	    }
	    catch(Ice.FacetNotExistException ex)
	    {
	    }
	}
	return d;
    }

    public static ProcessServerPrx
    uncheckedCast(Ice.ObjectPrx b)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
	    h.__copyFrom(b);
	    d = h;
	}
	return d;
    }

    public static ProcessServerPrx
    uncheckedCast(Ice.ObjectPrx b, String f)
    {
	ProcessServerPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    ProcessServerPrxHelper h = new ProcessServerPrxHelper();
	    h.__copyFrom(bb);
	    d = h;
	}
	return d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
	return new _ProcessServerDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
	return new _ProcessServerDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, ProcessServerPrx v)
    {
	__os.writeProxy(v);
    }

    public static ProcessServerPrx
    __read(IceInternal.BasicStream __is)
    {
	Ice.ObjectPrx proxy = __is.readProxy();
	if(proxy != null)
	{
	    ProcessServerPrxHelper result = new ProcessServerPrxHelper();
	    result.__copyFrom(proxy);
	    return result;
	}
	return null;
    }
}
