/*
 * Created on Sep 29, 2004
 * 
 * file: VirtualStackframe.java
 */
package ddproto1.debugger.managing.tracker;

import ddproto1.util.traits.commons.ConversionUtil;

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
    
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    private String outOp, inOp;
    private Integer ltid;
    private Integer callBase;
    private Integer callTop;
    private Byte ltgid;
    
    private String lastError;
    
    private ILocalThread componentThread;
    
    private boolean cleared = false;
    private boolean damaged = false;
   
    VirtualStackframe(String outOp, String inOp, Integer ltid, ILocalThread reference){
        this.outOp = outOp;
        this.inOp = inOp;
        this.ltid = ltid;
        this.ltgid = ct.guidFromUUID(ltid);
        this.callBase = new Integer(UNDEFINED);
        this.callTop = new Integer(UNDEFINED);
        this.componentThread = reference;
    }
    
    public void flagAsDamaged(){
        this.damaged = true;
    }
    
    public void flagAsDamaged(String reason){
        this.flagAsDamaged();
        this.lastError = reason;
    }
    
    public boolean isDamaged(){
        return damaged;
    }
    
    public String getLastError(){
        return lastError;
    }
    
    protected boolean isCleared(){
    	return cleared;
    }
    
    protected void clear(){
    	cleared = true;
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
    
    public ILocalThread getThreadReference(){
        return componentThread;
    }
    
    public Byte getLocalThreadNodeGID(){
        return ltgid;
    }
}
