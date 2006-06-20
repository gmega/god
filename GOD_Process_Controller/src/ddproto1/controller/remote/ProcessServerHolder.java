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

public final class ProcessServerHolder
{
    public
    ProcessServerHolder()
    {
    }

    public
    ProcessServerHolder(ProcessServer value)
    {
	this.value = value;
    }

    public class Patcher implements IceInternal.Patcher
    {
	public void
	patch(Ice.Object v)
	{
	    value = (ProcessServer)v;
	}

	public String
	type()
	{
	    return "::ddproto1::controller::remote::ProcessServer";
	}
    }

    public Patcher
    getPatcher()
    {
	return new Patcher();
    }

    public ProcessServer value;
}
