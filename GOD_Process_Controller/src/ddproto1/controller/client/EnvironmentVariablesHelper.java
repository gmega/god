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

public final class EnvironmentVariablesHelper
{
    public static void
    write(IceInternal.BasicStream __os, java.util.ArrayList __v)
    {
	if(__v == null)
	{
	    __os.writeSize(0);
	}
	else
	{
	    __os.writeSize(__v.size());
	    java.util.Iterator __i0 = __v.iterator();
	    while(__i0.hasNext())
	    {
		EnvironmentVariable __elem = (EnvironmentVariable)__i0.next();
		__elem.__write(__os);
	    }
	}
    }

    public static java.util.ArrayList
    read(IceInternal.BasicStream __is)
    {
	java.util.ArrayList __v;
	__v = new java.util.ArrayList();
	final int __len0 = __is.readSize();
	__is.startSeq(__len0, 2);
	for(int __i0 = 0; __i0 < __len0; __i0++)
	{
	    EnvironmentVariable __elem;
	    __elem = new EnvironmentVariable();
	    __elem.__read(__is);
	    __v.add(__elem);
	    __is.checkSeq();
	    __is.endElement();
	}
	__is.endSeq(__len0);
	return __v;
    }
}
