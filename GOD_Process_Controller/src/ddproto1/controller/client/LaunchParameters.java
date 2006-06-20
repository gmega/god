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

public final class LaunchParameters implements java.lang.Cloneable
{
    public String[] commandLine;

    public java.util.ArrayList envVars;

    public int pollInterval;

    public int maxUnflushedSize;

    public int flushTimeout;

    public LaunchParameters()
    {
    }

    public LaunchParameters(String[] commandLine, java.util.ArrayList envVars, int pollInterval, int maxUnflushedSize, int flushTimeout)
    {
	this.commandLine = commandLine;
	this.envVars = envVars;
	this.pollInterval = pollInterval;
	this.maxUnflushedSize = maxUnflushedSize;
	this.flushTimeout = flushTimeout;
    }

    public boolean
    equals(java.lang.Object rhs)
    {
	if(this == rhs)
	{
	    return true;
	}
	LaunchParameters _r = null;
	try
	{
	    _r = (LaunchParameters)rhs;
	}
	catch(ClassCastException ex)
	{
	}

	if(_r != null)
	{
	    if(!java.util.Arrays.equals(commandLine, _r.commandLine))
	    {
		return false;
	    }
	    if(envVars != _r.envVars && envVars != null && !envVars.equals(_r.envVars))
	    {
		return false;
	    }
	    if(pollInterval != _r.pollInterval)
	    {
		return false;
	    }
	    if(maxUnflushedSize != _r.maxUnflushedSize)
	    {
		return false;
	    }
	    if(flushTimeout != _r.flushTimeout)
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
	if(commandLine != null)
	{
	    for(int __i0 = 0; __i0 < commandLine.length; __i0++)
	    {
		if(commandLine[__i0] != null)
		{
		    __h = 5 * __h + commandLine[__i0].hashCode();
		}
	    }
	}
	if(envVars != null)
	{
	    __h = 5 * __h + envVars.hashCode();
	}
	__h = 5 * __h + pollInterval;
	__h = 5 * __h + maxUnflushedSize;
	__h = 5 * __h + flushTimeout;
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
	ParameterArrayHelper.write(__os, commandLine);
	EnvironmentVariablesHelper.write(__os, envVars);
	__os.writeInt(pollInterval);
	__os.writeInt(maxUnflushedSize);
	__os.writeInt(flushTimeout);
    }

    public void
    __read(IceInternal.BasicStream __is)
    {
	commandLine = ParameterArrayHelper.read(__is);
	envVars = EnvironmentVariablesHelper.read(__is);
	pollInterval = __is.readInt();
	maxUnflushedSize = __is.readInt();
	flushTimeout = __is.readInt();
    }
}
