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

public final class ControlClientHolder
{
    public
    ControlClientHolder()
    {
    }

    public
    ControlClientHolder(ControlClient value)
    {
	this.value = value;
    }

    public class Patcher implements IceInternal.Patcher
    {
	public void
	patch(Ice.Object v)
	{
	    value = (ControlClient)v;
	}

	public String
	type()
	{
	    return "::ddproto1::controller::client::ControlClient";
	}
    }

    public Patcher
    getPatcher()
    {
	return new Patcher();
    }

    public ControlClient value;
}
