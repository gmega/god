
/*
 * Created on Jul 27, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: DeferrableBreakpointRequest.java
 */

package ddproto1.debugger.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IVMThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.commons.ConversionTrait;

/**
 * @author giuliano
 *
 */
public class DeferrableBreakpointRequest implements IDeferrableRequest{

    private static final String FILTER_PROP = "DeferrableBreakpointRequest - Filter";
    
    public static final int NO_LINE = -1;
    
    public static final String module = "DeferrableBreakpointRequest -";
    
    private static final ConversionTrait ct = ConversionTrait.getInstance();
    
    private String vmid;
    private String classId;
    private String pattern;
    private String methodName;
    
    private List argumentList;
    private List <IDeferrableRequest.IPrecondition>preconList = new ArrayList<IDeferrableRequest.IPrecondition>();
    private List <IResolutionListener>listenerList = new LinkedList<IResolutionListener>();
    private List <ThreadReference> threadFilters = new LinkedList<ThreadReference>();
    
    private VMManagerFactory vmmf = VMManagerFactory.getInstance();
    
    // Eclipse is really annoying. If we don't declare the generic as <Object, Object> it complains.
    private Map <Object, Object> properties = new HashMap<Object, Object>();
    
    private boolean oneShot = false;
    private boolean resolved = false;
    private boolean end = false;
    private Location presetLocation;
    
    private int line_no;
    
    private DeferrableBreakpointRequest() { }

    public DeferrableBreakpointRequest(String vmid, String fullmethodname, List args) 
    	throws NoSuchSymbolException
    {
        this(vmid, fullmethodname, args, NO_LINE);
    }
    
    public DeferrableBreakpointRequest(String vmid, String className, int line_no)
        throws NoSuchSymbolException
    {
        this.vmid = vmid;
        this.classId = className;
        this.line_no = line_no;
        this.setPattern();
        this.setPreconditionList();
    }
    
    public DeferrableBreakpointRequest(String vmid, String fullmethodname, List args, int line_no)
    	throws NoSuchSymbolException
    {
        this.vmid = vmid;
        this.argumentList = (args == null)?new ArrayList():args;
        this.line_no = line_no;
        
        // Extracts the class name.
        if(fullmethodname == null) return;
        
        int idx = fullmethodname.lastIndexOf(".");
        if(idx == -1)
            throw new NoSuchSymbolException(module + " There cannot be a method outside a class.");
        classId = fullmethodname.substring(0, idx);
        this.setPattern();
        
        methodName = fullmethodname.substring(idx+1);
        
        setPreconditionList();
    }
    
    private void setPattern(){
        // Maybe the class id is a restricted regular expression ?
        if(classId.startsWith("*")){
            pattern = classId;
            end = true;
            classId = classId.substring(1, classId.length());
        }else if(classId.endsWith("*")){
            pattern = classId;
            end = false;
            classId = classId.substring(0, classId.length() - 1);
        }
    }
    
    public DeferrableBreakpointRequest(Location l, String vmid){
        preconList.add(DeferrableRequestQueue.nullPrecondition);
        presetLocation = l;
        this.vmid = vmid;
    }

    private void setPreconditionList(){
        StdPreconditionImpl connect = new StdPreconditionImpl();
        connect.setType(new StdTypeImpl(IDeferrableRequest.VM_CONNECTION, IDeferrableRequest.MATCH_ONCE));
        
        StdPreconditionImpl load = new StdPreconditionImpl();
        load.setType(new StdTypeImpl(IDeferrableRequest.CLASSLOADING, (pattern == null)?IDeferrableRequest.MATCH_ONCE:IDeferrableRequest.MATCH_MULTIPLE));
        load.setClassId((pattern == null)?classId:pattern);
        
        preconList.add(connect);
        preconList.add(load);
    }
    
    public List <IDeferrableRequest.IPrecondition> getRequirements(){
        return preconList;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.DeferrableEventRequest#resolveNow(com.sun.jdi.ReferenceType)
     */
    public synchronized Object resolveNow(IDeferrableRequest.IResolutionContext rc) 
    	throws Exception
    {
    	ReferenceType rt;
    	ThreadReference specific;
    	VirtualMachine vm;
    	
    	checkResolved();
    	
        /* First precondition has been met - we might be able to set the breakpoint. */
    	if(rc.getPrecondition().getType().eventType() == IDeferrableRequest.VM_CONNECTION){
    		vm = getAssociatedVM();
            
            if(pattern == null){
                /* Resolution will fail only if this method throws an exception 
                 * That's why there's no returning null here.*/
                rt = checkLoaded(classId);
                
                // TODO Loaded classes are fulfilled preconditions and should no longer
                //      be part of the advertised preconditions.
            
                /* We must ask the VM to notify us when our target class actually
                 * gets loaded. */
                if(rt == null){
                    DeferrableClassPrepareRequest cpr = new DeferrableClassPrepareRequest(vmid);
                    cpr.addClassFilter(classId);
                    VirtualMachineManager vmm = vmmf.getVMManager(vmid);
                    vmm.getDeferrableRequestQueue().addEagerlyResolve(cpr);
                    return cpr;
                }else{
                    return this.resolveWith(rt);
                }
            }
            // If it's a pattern breakpoint, we'll have to try it against all classes
            // matching the pattern.
            else{
                for(ReferenceType candidate : getAllLoaded()){
                    Object request = resolveWith(candidate);
                    if(request != null) return request;
                }
                
                ClassPrepareRequest inserted = null;
                
                /* We now check if there's a class prepare request for our pattern already */
                for(ClassPrepareRequest cpr : vm.eventRequestManager().classPrepareRequests()){
                    String filter = (String)cpr.getProperty(FILTER_PROP);
                    if(filter == null) continue;
                    if(filter.equals(pattern)){
                        inserted = cpr;
                        break;
                    }
                }
                
                if(inserted == null){
                    ClassPrepareRequest cpr = vm.eventRequestManager().createClassPrepareRequest();
                    cpr.addClassFilter(pattern);
                    cpr.addCountFilter(1);
                    cpr.enable();
                    cpr.putProperty(FILTER_PROP, pattern);
                    inserted = cpr;
                }
                
                return inserted;
            }
         
        }
        /* Second precondition has been met - we do a new attempt. */
    	else if(rc.getPrecondition().getType().eventType() == IDeferrableRequest.CLASSLOADING){
            String true_clsid;
            
            /* Since with patterns we cannot check consistency by just trying to acquire the 
             * reference type (since it doesn't exist), we do it by matching the pattern against
             * the class reported loaded.
             */
            if(pattern != null){
                String loaded = rc.getPrecondition().getClassId();
                if((end && !loaded.endsWith(classId)) || (!end && !loaded.startsWith(classId))){
                    throw new InternalError("Reported class not expected - " + loaded);
                }
                true_clsid = loaded;
            }else{
                true_clsid = classId;
            }
        	rt = checkLoaded(true_clsid);
        	if(rt == null) return null; // Resolution failed.
            
            return this.resolveWith(rt);
        }
    	
    	/* This precondition is advertised by deferrable breakpoint requests
    	 * that are created with the constructor that takes a location as a
    	 * parameter.
    	 */
    	else if(rc.getPrecondition().getType().eventType() == IDeferrableRequest.NIL){
            if(presetLocation == null)
                throw new InternalError("Precondition NONE is only valid for Location breakpoints.");
            else return setForLocation(presetLocation, getAssociatedVM());
    	}else{
        	throw new InternalError("Unrecognized precondition.");
        }
    }
    
    private Object resolveWith(ReferenceType rt)
        throws Exception
    {
        
        VirtualMachine vm = getAssociatedVM();
        Location breakLocation = null;
        // Two types of breakpoints.
        // 1 - Method breakpoints
        if(methodName != null){
            Method method = findMethod(rt);
            
            // If no line was specified, stops as soon as the method starts. 
            if(line_no < 0){
                breakLocation = method.location();
            }
            // Otherwise treats the line number as an offset into the method's source code.
            else{
                List locs = method.allLineLocations();
                if(line_no >= locs.size()){
                    throw new IllegalAttributeException(module + " Cannot locate line " + line_no + " in method " + method.name());
                }
                breakLocation = (Location)locs.get(line_no);
            }
        }
        // 2 - Absolute line breakpoints.
        else{
            List locations = rt.locationsOfLine(line_no);
            if(locations.size() == 0){
                /* Well, the line doesn't exist. If we're a pattern breakpoint we do nothing. Otherwise it 
                 * means one of two things: 1) the user screwed up 2) we're an absolute line breakpoint meant
                 * to be set in an internal class. */
                if(pattern == null){
                    DeferrableBreakpointRequest dbr = new DeferrableBreakpointRequest(vmid, classId + "*", line_no);
                    VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vmid);
                    vmm.getDeferrableRequestQueue().addEagerlyResolve(dbr);
                
                    /* Returns something just to get us out of the deferred queue (we'll be replaced by the 
                     *  new request we've just made).
                     */
                
                    return dbr;
                }else{
                    return null; /* Not this time, Jimbo. */
                }
            }
            
            if(locations.size() > 1)
                throw new InternalError(module + " Unexpected location size - unsupported.");
            
            breakLocation = (Location)locations.get(0);
        }
        
        // Finally, sets the breakpoint.
        return setForLocation(breakLocation, vm);
    }
    
    private BreakpointRequest setForLocation(Location loc, VirtualMachine vm){
        BreakpointRequest br = vm.eventRequestManager().createBreakpointRequest(loc);
        PolicyManager pm = PolicyManager.getInstance();
        br.setSuspendPolicy(pm.getPolicy("request.breakpoint"));
        if(oneShot)
            br.addCountFilter(1);
        Iterator it = threadFilters.iterator();
        while(it.hasNext())
            br.addThreadFilter((ThreadReference)it.next());
        br.enable();

        br.putProperty(DebuggerConstants.VMM_KEY, vmid);
        
        for(Object key : properties.keySet()){
            br.putProperty(key, properties.get(key));
        }

        resolved = true;
        /* We are done. Notify whomever could be interested in this resolution. */
        broadcastToListeners(this, br);
        
        return br;
    }
    
    public List<ReferenceType> getAllLoaded(){
        VirtualMachine vm = this.getAssociatedVM();
        List<ReferenceType> lst = new LinkedList<ReferenceType>();
        
        for(ReferenceType rt : vm.allClasses()){
            if(end && rt.name().endsWith(classId)) lst.add(rt);
            else if(!end && rt.name().startsWith(classId)) lst.add(rt);
        }
        
        return lst;
    }
    
    public ReferenceType checkLoaded(String clsid){
    	VirtualMachine vm = this.getAssociatedVM();
    	Iterator it = vm.allClasses().iterator();
        
    	while(it.hasNext()){
    		ReferenceType rt = (ReferenceType)it.next();
    		if(rt.name().equals(clsid)){
    			return rt;
    		}
    	}
    	return null;
    }
    
    /** This method will attempt to match the method specified in the breakpoint
     * request with the ones returned by the JDI ReferenceType. It is a lot of work
     * for very small (but crucial) results. :-) 
     * 
     * @param in
     * @return
     */
    private Method findMethod(ReferenceType in) throws NoSuchSymbolException,
            AmbiguousSymbolException {
        
        // Qualify the args for use during comparison.
        Iterator it = argumentList.iterator();
        List <String> l = new ArrayList<String>();
        for (int i = 0; it.hasNext(); i++) {
            String arg = (String) it.next();
            arg = qualifyArg(arg);
            l.add(i, arg);
        }

        Method candidate = null;

        // Search for the required method.
        it = in.methods().iterator();
        while (it.hasNext()) {
            Method method = (Method) it.next();
            // Got method with the required name.
            if (method.name().equals(methodName)) {
                // Now compare the arguments
                if (compareArgs(method, l)) {
                    if (candidate != null)
                        throw new AmbiguousSymbolException(module
                                + " Cannot resolve method " + methodName
                                + " unanbiguously.");
                    else {
                        candidate = method;
                    }
                }
            }
        }

        if (candidate == null)
            throw new NoSuchSymbolException(module + " Could not find method "
                    + methodName + " in reference type (class?) " + in);

        return candidate;
    }
    
    /** Compare arguments of the method passed as parameter with the 
     * arguments of the method specified for this breakpoint. Implements
     * a semantic that allows the user to ommit the argument list if
     * the method can be resolved unambiguously.
     * 
     * @param m
     * @return
     */
    private boolean compareArgs(Method m, List <String> argList){
        // This might sound bizarre, but works well.
        if(argList.isEmpty())
            return true;
        
        List args = m.argumentTypeNames();
        
        /* This heuristic could save time (if argument lists
         * differ in size then the methods are different for sure).
         */ 
        if(argList.size() != args.size())
            return false;
      
        /* We have to guarantee we are running through the lists in the
         * same order
         */ 
        for(int i = 0; i < argList.size(); i++){
            String arg1 = (String)argList.get(i);
            String arg2 = (String)args.get(i);
            if(!arg1.equals(arg2))
                return false;
        }
        
        // If we got so far, then they must be equal.
        return true;
    }

    private String qualifyArg(String arg) throws NoSuchSymbolException,
            AmbiguousSymbolException {
        /*
         * We assume very strict argument syntax (and we can do so because of
         * the lexer we're using).
         */
        String normalized = arg;
        String append = "";
        if (arg.endsWith("[]")) {
            normalized = arg.replaceAll("\"[]\"", "");
            append = "[]";
        }

        // Attempts to qualify this argument
        // First, tries to resolve directly
        List clist = getAssociatedVM().classesByName(normalized);
        if (clist.isEmpty()) {

            // Perhaps the class name is just incomplete?
            if (normalized.indexOf('.') == -1) {
                Iterator it = getAssociatedVM().allClasses().iterator();
                String candidate = null;

                // We'll just have to look.
                while (it.hasNext()) {
                    String klass = (String) it.next();
                    if (klass.endsWith(normalized)) {
                        if (candidate == null) {
                            candidate = klass;
                        } else {
                            throw new AmbiguousSymbolException(
                                    module
                                            + " Class "
                                            + normalized
                                            + " cannot be resolved unanbiguosly. Please specify a fully qualified name.");
                        }
                    }
                }

                if (candidate == null)
                    throw new NoSuchSymbolException(module
                            + " Cannot resolve type " + normalized);

                normalized = candidate;
            }
            // Sorry. This class just doesn't exist.
            else {
                throw new NoSuchSymbolException(module
                        + " Cannot resolve type " + normalized);
            }
        } else {
            /*
             * This has no practical implications since we're just gathering
             * names, but it could be worth warning since we've detected it
             * anyways.
             */
            if (clist.size() >= 2) {
                MessageHandler mh = MessageHandler.getInstance();
                mh
                        .getWarningOutput()
                        .println(
                                module
                                        + " Class "
                                        + normalized
                                        + " resolved to two different reference types \n "
                                        + "with the same fully-qualified name. Currently there's "
                                        + "no support for selecting \n between them.");
            }
        }

        return (normalized + append);
    }
    
    public synchronized void addThreadFilter(Integer lt_uuid)
    	throws NoSuchSymbolException
    {
        checkResolved();
    	VirtualMachineManager vmm = VMManagerFactory.getInstance().getVMManager(vmid);
    	/* First checks if there's a thread filter applied and if it's actually valid. */
    	IVMThreadManager tm = vmm.getThreadManager();
    	ThreadReference tr = tm.findThreadByUUID(lt_uuid);
    	if(tr == null){
    	    throw new NoSuchSymbolException("Can't find thread " + ct.uuid2Dotted(lt_uuid.intValue()));
    	}
        threadFilters.add(tr);
    }
    
    public synchronized void putProperty(Object key, Object val){
        checkResolved();
        properties.put(key, val);
    }
    
    public synchronized Object getProperty(Object key, Object val){
        checkResolved();
        return properties.get(key);
    }
    
    public synchronized void setOneShot(boolean oneShot){
        checkResolved();
        this.oneShot = oneShot;
    }
    
    public synchronized boolean isOneShot(){
        return oneShot;
    }
    

    private void checkResolved(){
        if(resolved)
            throw new IllegalStateException("Operation invalid on resolved event request.");
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.DeferrableEventRequest#getAssociatedVM()
     */
    protected VirtualMachine getAssociatedVM() 
    	throws VMDisconnectedException
    {
        return VMManagerFactory.getInstance().getVMManager(vmid).virtualMachine();
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#addResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void addResolutionListener(IResolutionListener listener) {
        listenerList.add(listener);
    }

    /* (non-Javadoc)
     * @see ddproto1.debugger.request.IDeferrableRequest#removeResolutionListener(ddproto1.debugger.request.IResolutionListener)
     */
    public void removeResolutionListener(IResolutionListener listener) {
        listenerList.remove(listener);        
    }
    
    private void broadcastToListeners(IDeferrableRequest req, EventRequest ereq){
        Iterator it = listenerList.iterator();
        while(it.hasNext()){
            IResolutionListener listener = (IResolutionListener)it.next();
            listener.notifyResolution(req, ereq);
        }
    }
}
