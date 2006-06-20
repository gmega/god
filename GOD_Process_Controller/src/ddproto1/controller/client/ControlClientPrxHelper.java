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

public final class ControlClientPrxHelper extends Ice.ObjectPrxHelperBase implements ControlClientPrx
{
    public void
    notifyProcessDeath(int pHandle)
    {
	notifyProcessDeath(pHandle, __defaultContext());
    }

    public void
    notifyProcessDeath(int pHandle, java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_ControlClientDel __del = (_ControlClientDel)__delBase;
		__del.notifyProcessDeath(pHandle, __ctx);
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

    public void
    notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle)
    {
	notifyProcessDeath_async(__cb, pHandle, __defaultContext());
    }

    public void
    notifyProcessDeath_async(AMI_ControlClient_notifyProcessDeath __cb, int pHandle, java.util.Map __ctx)
    {
	__cb.__invoke(this, __cb, pHandle, __ctx);
    }

    public void
    notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer)
    {
	notifyServerUp(procServer, __defaultContext());
    }

    public void
    notifyServerUp(ddproto1.controller.remote.ProcessServerPrx procServer, java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_ControlClientDel __del = (_ControlClientDel)__delBase;
		__del.notifyServerUp(procServer, __ctx);
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

    public void
    receiveStringFromSTDERR(int pHandle, String data)
    {
	receiveStringFromSTDERR(pHandle, data, __defaultContext());
    }

    public void
    receiveStringFromSTDERR(int pHandle, String data, java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_ControlClientDel __del = (_ControlClientDel)__delBase;
		__del.receiveStringFromSTDERR(pHandle, data, __ctx);
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

    public void
    receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data)
    {
	receiveStringFromSTDERR_async(__cb, pHandle, data, __defaultContext());
    }

    public void
    receiveStringFromSTDERR_async(AMI_ControlClient_receiveStringFromSTDERR __cb, int pHandle, String data, java.util.Map __ctx)
    {
	__cb.__invoke(this, __cb, pHandle, data, __ctx);
    }

    public void
    receiveStringFromSTDOUT(int pHandle, String data)
    {
	receiveStringFromSTDOUT(pHandle, data, __defaultContext());
    }

    public void
    receiveStringFromSTDOUT(int pHandle, String data, java.util.Map __ctx)
    {
	int __cnt = 0;
	while(true)
	{
	    try
	    {
		Ice._ObjectDel __delBase = __getDelegate();
		_ControlClientDel __del = (_ControlClientDel)__delBase;
		__del.receiveStringFromSTDOUT(pHandle, data, __ctx);
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

    public void
    receiveStringFromSTDOUT_async(AMI_ControlClient_receiveStringFromSTDOUT __cb, int pHandle, String data)
    {
	receiveStringFromSTDOUT_async(__cb, pHandle, data, __defaultContext());
    }

    public void
    receiveStringFromSTDOUT_async(AMI_ControlClient_receiveStringFromSTDOUT __cb, int pHandle, String data, java.util.Map __ctx)
    {
	__cb.__invoke(this, __cb, pHandle, data, __ctx);
    }

    public static ControlClientPrx
    checkedCast(Ice.ObjectPrx b)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (ControlClientPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::client::ControlClient"))
		{
		    ControlClientPrxHelper h = new ControlClientPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static ControlClientPrx
    checkedCast(Ice.ObjectPrx b, java.util.Map ctx)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    try
	    {
		d = (ControlClientPrx)b;
	    }
	    catch(ClassCastException ex)
	    {
		if(b.ice_isA("::ddproto1::controller::client::ControlClient", ctx))
		{
		    ControlClientPrxHelper h = new ControlClientPrxHelper();
		    h.__copyFrom(b);
		    d = h;
		}
	    }
	}
	return d;
    }

    public static ControlClientPrx
    checkedCast(Ice.ObjectPrx b, String f)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::client::ControlClient"))
		{
		    ControlClientPrxHelper h = new ControlClientPrxHelper();
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

    public static ControlClientPrx
    checkedCast(Ice.ObjectPrx b, String f, java.util.Map ctx)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    try
	    {
		if(bb.ice_isA("::ddproto1::controller::client::ControlClient", ctx))
		{
		    ControlClientPrxHelper h = new ControlClientPrxHelper();
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

    public static ControlClientPrx
    uncheckedCast(Ice.ObjectPrx b)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    ControlClientPrxHelper h = new ControlClientPrxHelper();
	    h.__copyFrom(b);
	    d = h;
	}
	return d;
    }

    public static ControlClientPrx
    uncheckedCast(Ice.ObjectPrx b, String f)
    {
	ControlClientPrx d = null;
	if(b != null)
	{
	    Ice.ObjectPrx bb = b.ice_newFacet(f);
	    ControlClientPrxHelper h = new ControlClientPrxHelper();
	    h.__copyFrom(bb);
	    d = h;
	}
	return d;
    }

    protected Ice._ObjectDelM
    __createDelegateM()
    {
	return new _ControlClientDelM();
    }

    protected Ice._ObjectDelD
    __createDelegateD()
    {
	return new _ControlClientDelD();
    }

    public static void
    __write(IceInternal.BasicStream __os, ControlClientPrx v)
    {
	__os.writeProxy(v);
    }

    public static ControlClientPrx
    __read(IceInternal.BasicStream __is)
    {
	Ice.ObjectPrx proxy = __is.readProxy();
	if(proxy != null)
	{
	    ControlClientPrxHelper result = new ControlClientPrxHelper();
	    result.__copyFrom(proxy);
	    return result;
	}
	return null;
    }
}
