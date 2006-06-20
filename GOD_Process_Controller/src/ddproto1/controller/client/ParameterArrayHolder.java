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

public final class ParameterArrayHolder
{
    public
    ParameterArrayHolder()
    {
    }

    public
    ParameterArrayHolder(String[] value)
    {
	this.value = value;
    }

    public String[] value;
}
