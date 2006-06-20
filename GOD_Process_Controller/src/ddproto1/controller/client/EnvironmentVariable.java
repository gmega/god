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

public final class EnvironmentVariable implements java.lang.Cloneable
{
    public String key;

    public String value;

    public EnvironmentVariable()
    {
    }

    public EnvironmentVariable(String key, String value)
    {
	this.key = key;
	this.value = value;
    }

    public boolean
    equals(java.lang.Object rhs)
    {
	if(this == rhs)
	{
	    return true;
	}
	EnvironmentVariable _r = null;
	try
	{
	    _r = (EnvironmentVariable)rhs;
	}
	catch(ClassCastException ex)
	{
	}

	if(_r != null)
	{
	    if(key != _r.key && key != null && !key.equals(_r.key))
	    {
		return false;
	    }
	    if(value != _r.value && value != null && !value.equals(_r.value))
	    {
		return false;
	    }

	    return true;
	}

	return false;
    }

    public int
    hashCode()
    {
	int __h = 0;
	if(key != null)
	{
	    __h = 5 * __h + key.hashCode();
	}
	if(value != null)
	{
	    __h = 5 * __h + value.hashCode();
	}
	return __h;
    }

    public java.lang.Object
    clone()
    {
	java.lang.Object o = null;
	try
	{
	    o = super.clone();
	}
	catch(CloneNotSupportedException ex)
	{
	    assert false; // impossible
	}
	return o;
    }

    public void
    __write(IceInternal.BasicStream __os)
    {
	__os.writeString(key);
	__os.writeString(value);
    }

    public void
    __read(IceInternal.BasicStream __is)
    {
	key = __is.readString();
	value = __is.readString();
    }
}
