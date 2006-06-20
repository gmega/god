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

public final class RemoteProcessPrxHelper extends Ice.ObjectPrxHelperBase implements RemoteProcessPrx
{
    public void
    dispose()
    {
	dispose(__defaultContext());
    }

    public void
    dispose(java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_RemoteProcessDel __del = (_RemoteProcessDel)__delBase;
		__del.dispose(__ctx);
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

    public int
    getHandle()
    {
	return getHandle(__defaultContext());
    }

    public int
    getHandle(java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		__checkTwowayOnly("getHandle");
		Ice._ObjectDel __delBase = __getDelegate();
		_RemoteProcessDel __del = (_RemoteProcessDel)__delBase;
		return __del.getHandle(__ctx);
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
		_RemoteProcessDel __del = (_RemoteProcessDel)__delBase;
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

    public void
    writeToSTDIN(String message)
	throws ServerRequestException
    {
	writeToSTDIN(message, __defaultContext());
    }

    public void
    writeToSTDIN(String message, java.util.Map __ctx)
	throws ServerRequestException
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		__checkTwowayOnly("writeToSTDIN");
		Ice._ObjectDel __delBase = __getDelegate();
		_RemoteProcessDel __del = (_RemoteProcessDel)__delBase;
		__del.writeToSTDIN(message, __ctx);
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

    public static RemoteProcessPrx
    checkedCast(Ice.ObjectPrx b)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (RemoteProcessPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::remote::RemoteProcess"))
		{
		    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static RemoteProcessPrx
    checkedCast(Ice.ObjectPrx b, java.util.Map ctx)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (RemoteProcessPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::remote::RemoteProcess", ctx))
		{
		    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static RemoteProcessPrx
    checkedCast(Ice.ObjectPrx b, String f)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::remote::RemoteProcess"))
		{
		    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
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

    public static RemoteProcessPrx
    checkedCast(Ice.ObjectPrx b, String f, java.util.Map ctx)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::remote::RemoteProcess", ctx))
		{
		    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
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

    public static RemoteProcessPrx
    uncheckedCast(Ice.ObjectPrx b)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
	    h.__copyFrom(b);
	    d = h;
	}
	return d;
    }

    public static RemoteProcessPrx
    uncheckedCast(Ice.ObjectPrx b, String f)
    {
	RemoteProcessPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    RemoteProcessPrxHelper h = new RemoteProcessPrxHelper();
	    h.__copyFrom(bb);
	    d = h;
	}
	return d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
	return new _RemoteProcessDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
	return new _RemoteProcessDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, RemoteProcessPrx v)
    {
	__os.writeProxy(v);
    }

    public static RemoteProcessPrx
    __read(IceInternal.BasicStream __is)
    {
	Ice.ObjectPrx proxy = __is.readProxy();
	if(proxy != null)
	{
	    RemoteProcessPrxHelper result = new RemoteProcessPrxHelper();
	    result.__copyFrom(proxy);
	    return result;
	}
	return null;
    }
}
