/*
 * Created on Sep 29, 2004
 * 
 * file: VirtualStackframe.java
 */
package ddproto1.debugger.managing.tracker;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.commons.IConfigurationConstants;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * The virtual stack frame is not much more than a lightweigth marker of 
 * the application thread boundaries.
 * 
 * 
 * @author giuliano
 *
 */
public class VirtualStackframe extends DebugElement implements IStackFrame{
    
    public static final int UNDEFINED = -1;
    
    private static final Logger logger = MessageHandler.getInstance().getLogger(VirtualStackframe.class);
    
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    private String fOutputOperation;
    private String fInputOperation;
    
    private Integer callBase;
    private Integer callTop;
    
    private String fLastError;
    
    private volatile ILocalThread fComponentThread;
    
    private final AtomicReference<DistributedThread> fParentDT =
        new AtomicReference<DistributedThread>();
    
    private boolean cleared = false;
    private boolean damaged = false;
   
    protected VirtualStackframe(String outOp, String inOp, ILocalThread reference, IDebugTarget parentTarget){
        super(parentTarget);
        this.fOutputOperation = outOp;
        this.fInputOperation = inOp;
        this.callBase = new Integer(UNDEFINED);
        this.callTop = new Integer(UNDEFINED);
        this.fComponentThread = reference;
    }
    
    protected void setParent(DistributedThread parentDT){
        if(!fParentDT.compareAndSet(null, parentDT))
            throw new IllegalStateException("Virtual stackframe already belongs to DT " + parentDT);
    }
    
    public void flagAsDamaged(){
        this.damaged = true;
    }
    
    public void flagAsDamaged(String reason){
        this.flagAsDamaged();
        this.fLastError = reason;
    }
    
    public boolean isDamaged(){
        return damaged;
    }
    
    public String getLastError(){
        return fLastError;
    }
    
    protected boolean isCleared(){
    	return cleared;
    }
    
    protected void clear(){
    	cleared = true;
    }
    
    public String getOutboundOperation(){
        return fOutputOperation;
    }
    
    public String getInboundOperation(){
        return fInputOperation;
    }
    
    public Integer getLocalThreadId(){
        return getThreadReference().getGUID();
    }
    
    protected void setCallBase(Integer callBase){
        this.callBase = callBase;
    }
    
    protected void setCallTop(Integer callTop){
    	this.callTop = callTop;
    }
    
    protected void setInboundOperation(String op){
        this.fInputOperation = op;
    }
    
    protected void setOutboundOperation(String op){
        this.fOutputOperation = op;
    }
    
    public Integer getCallBase(){
        return callBase;
    }
    
    public Integer getCallTop(){
    	return callTop;
    }
    
    public ILocalThread getThreadReference(){
        return fComponentThread;
    }
    
    public Byte getLocalThreadNodeGID(){
        return ct.guidFromUUID(getLocalThreadId());
    }
    
    private DistributedThread getParentDT(){
        return fParentDT.get();
    }

    public IThread getThread() {
        return getParentDT();
    }

    /** Virtual stack frames have no variables. */
    public IVariable[] getVariables() throws DebugException {
        return new IVariable[0];
    }

    public boolean hasVariables() throws DebugException {
        return false;
    }

    /** Virtual stack frames also have no location information at all. */
    public int getLineNumber() throws DebugException {
        return -1;
    }

    public int getCharStart() throws DebugException {
        return -1;
    }

    public int getCharEnd() throws DebugException {
        return -1;
    }

    /** The name of a virtual stack frame is the same name
     * as the name of the thread it encompasses.
     */
    public String getName() throws DebugException {
        return getThreadReference().getName();
    }

    /** Virtual stack frames also have no register groups */
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return new IRegisterGroup[0];
    }

    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    /** Model identifier is the same as for the rest of the plug-in */
    public String getModelIdentifier() {
        return GODBasePlugin.getDefault().getBundle().getSymbolicName();
    }

    public Object getAdapter(Class adapter) {
        if(adapter.isAssignableFrom(VirtualStackframe.class))
            return this;
        return null;
    }

    /** You cannot actually perform any stepping operations
     * over virtual stackframes.
     */
    public boolean canStepInto() {
        return false;
    }

    public boolean canStepOver() {
        return false;
    }

    public boolean canStepReturn() {
        return false;
    }

    public boolean isStepping() {
        return false;
    }

    public void stepInto() throws DebugException {
        GODBasePlugin.throwDebugException("Cannot step a running thread.");
    }

    public void stepOver() throws DebugException {
        GODBasePlugin.throwDebugException("Cannot step a running thread.");
    }

    public void stepReturn() throws DebugException {
        GODBasePlugin.throwDebugException("Cannot step a running thread.");
    }

    public boolean canResume() {
        return getThreadReference().canResume();
    }

    public boolean canSuspend() {
        return getThreadReference().canSuspend();
    }

    /** Should never be suspended. */
    public boolean isSuspended() {
        boolean val = getThreadReference().isSuspended();
        if(val){
            logger.error("Virtual stack frame queried for " +
                    "suspension status while suspended.");
        }
        
        return val;
    }

    public void resume() throws DebugException {
        GODBasePlugin.throwDebugException("Cannot resume a virtual stack frame.");
    }

    public void suspend() throws DebugException {
        getThreadReference().suspend();
    }

    public boolean canTerminate() {
        return getThreadReference().canTerminate();
    }

    public boolean isTerminated() {
        return getThreadReference().isTerminated();
    }

    public void terminate() throws DebugException { 
        getThreadReference().terminate();
    }
}
