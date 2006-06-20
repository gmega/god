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

public abstract class AMI_ControlClient_notifyProcessDeath extends IceInternal.OutgoingAsync
{
    public abstract void ice_response();
    public abstract void ice_exception(Ice.LocalException ex);

    public final void
    __invoke(Ice.ObjectPrx __prx, AMI_ControlClient_notifyProcessDeath __cb, int pHandle, java.util.Map __ctx)
    {
	try
	{
	    __prepare(__prx, "notifyProcessDeath", Ice.OperationMode.Normal, __ctx);
	    __os.writeInt(pHandle);
	    __os.endWriteEncaps();
	}
	catch(Ice.LocalException __ex)
	{
	    __finished(__ex);
	    return;
	}
	__send();
    }

    protected final void
    __response(boolean __ok)
    {
	try
	{
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
	    __finished(__ex);
	    return;
	}
	ice_response();
    }
}
