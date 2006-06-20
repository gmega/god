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

public class ServerRequestException extends Ice.UserException
{
    public ServerRequestException()
    {
    }

    public ServerRequestException(String reason, int errorCode)
    {
	this.reason = reason;
	this.errorCode = errorCode;
    }

    public String
    ice_name()
    {
	return "ddproto1::controller::remote::ServerRequestException";
    }

    public String reason;

    public int errorCode;

    public void
    __write(IceInternal.BasicStream __os)
    {
	__os.writeString("::ddproto1::controller::remote::ServerRequestException");
	__os.startWriteSlice();
	__os.writeString(reason);
	__os.writeInt(errorCode);
	__os.endWriteSlice();
    }

    public void
    __read(IceInternal.BasicStream __is, boolean __rid)
    {
	if(__rid)
	{
	    String myId = __is.readString();
	}
	__is.startReadSlice();
	reason = __is.readString();
	errorCode = __is.readInt();
	__is.endReadSlice();
    }

    public void
    __write(Ice.OutputStream __outS)
    {
	Ice.MarshalException ex = new Ice.MarshalException();
	ex.reason = "exception ddproto1::controller::remote::ServerRequestException was not generated with stream support";
	throw ex;
    }

    public void
    __read(Ice.InputStream __inS, boolean __rid)
    {
	Ice.MarshalException ex = new Ice.MarshalException();
	ex.reason = "exception ddproto1::controller::remote::ServerRequestException was not generated with stream support";
	throw ex;
    }
}
