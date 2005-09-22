/*
 * Created on Sep 29, 2004
 * 
 * file: VirtualStackframe.java
 */
package ddproto1.debugger.managing.tracker;

import ddproto1.util.traits.commons.ConversionTrait;

/**
 * The virtual stack frame is not much more than a lightweigth marker of 
 * the application thread boundaries.
 * 
 * 
 * @author giuliano
 *
 */
public class VirtualStackframe {
    
    public static final int UNDEFINED = -1;
    
    private static final ConversionTrait ct = ConversionTrait.getInstance();
    
    private String outOp, inOp;
    private Integer ltid;
    private Integer callBase;
    private Integer callTop;
    private Byte ltgid;
   
    VirtualStackframe(String outOp, String inOp, Integer ltid){
        this.outOp = outOp;
        this.inOp = inOp;
        this.ltid = ltid;
        this.ltgid = ct.guidFromUUID(ltid);
        this.callBase = new Integer(UNDEFINED);
        this.callTop = new Integer(UNDEFINED);
    }
    
    public String getOutboundOperation(){
        return outOp;
    }
    
    public String getInboundOperation(){
        return inOp;
    }
    
    public Integer getLocalThreadId(){
        return ltid;
    }
    
    protected void setCallBase(Integer callBase){
        this.callBase = callBase;
    }
    
    protected void setCallTop(Integer callTop){
    	this.callTop = callTop;
    }
    
    protected void setInboundOperation(String op){
        this.inOp = op;
    }
    
    protected void setOutboundOperation(String op){
        this.outOp = op;
    }
    
    public Integer getCallBase(){
        return callBase;
    }
    
    public Integer getCallTop(){
    	return callTop;
    }
    
    public Byte getLocalThreadNodeGID(){
        return ltgid;
    }
}
