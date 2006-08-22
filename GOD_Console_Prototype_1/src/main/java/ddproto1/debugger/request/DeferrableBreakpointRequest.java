
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;

import ddproto1.commons.DebuggerConstants;
import ddproto1.debugger.managing.IJavaNodeManager;
import ddproto1.debugger.managing.IVMManagerFactory;
import ddproto1.debugger.managing.IJavaThreadManager;
import ddproto1.debugger.managing.VMManagerFactory;
import ddproto1.debugger.managing.VirtualMachineManager;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.InternalError;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.TargetRequestFailedException;
import ddproto1.util.MessageHandler;
import ddproto1.util.PolicyManager;
import ddproto1.util.traits.commons.ConversionUtil;

/**
 * @author giuliano
 * 
 * Buggy: this thing wasn't conceived with multiple classloaders in mind. It must be
 *        rewritten, together with the whole DeferrableRequestQueue.
 *
 */
public class DeferrableBreakpointRequest extends AbstractDeferrableRequest{

    public static final int NO_LINE = -1;
    
    public static final String module = "DeferrableBreakpointRequest -";
    
    private static final ConversionUtil ct = ConversionUtil.getInstance();
    
    private String classId;
    private String pattern;
    private String methodName;
    
    private List argumentList;
    private List <IDeferrableRequest.IPrecondition>preconList = new ArrayList<IDeferrableRequest.IPrecondition>();
    private List <ThreadReference> threadFilters = new LinkedList<ThreadReference>();
    
    private IVMManagerFactory vmmf = VMManagerFactory.getInstance();
    
    // Eclipse is really annoying. If we don't declare the generic as <Object, Object> it complains.
    private Map <Object, Object> properties = new HashMap<Object, Object>();
    
    private boolean oneShot = false;
    private boolean end = false;
    private Location presetLocation;
    
    private int line_no;
    
    private DeferrableBreakpointRequest spawn;
    private Cancelable loadRequest;
    private List<Cancelable> jdiRequests;
    private Set<ReferenceType> targeted;
    
    public DeferrableBreakpointRequest(String vmid, String fullmethodname, List args) 
    	throws NoSuchSymbolException
    {
        this(vmid, fullmethodname, args, NO_LINE);
    }
    
    public DeferrableBreakpointRequest(String vmid, String className, int line_no)
    {
        super(vmid);
        this.classId = className;
        this.line_no = line_no;
        this.init();
    }
    
    private DeferrableBreakpointRequest(String vmid, Map <Object, Object>properties, List <ThreadReference>threadFilters, String classId, int line_no){
        super(vmid);
        this.properties = properties;
        this.threadFilters = threadFilters;
        this.classId = classId;
        this.line_no = line_no;
        this.init();
    }
    
    public DeferrableBreakpointRequest(String vmid, String fullmethodname, List args, int line_no)
    	throws NoSuchSymbolException
    {
        super(vmid);
        this.argumentList = (args == null)?new ArrayList():args;
        this.line_no = line_no;
        
        // Extracts the class name.
        if(fullmethodname == null) return;
        
        int idx = fullmethodname.lastIndexOf(".");
        if(idx == -1)
            throw new NoSuchSymbolException(module + " There cannot be a method outside a class.");
        classId = fullmethodname.substring(0, idx);
        methodName = fullmethodname.substring(idx+1);
        
        this.init();
    }
    
    private void init(){
        this.spawn = this;
        this.jdiRequests = new LinkedList<Cancelable>();
        this.targeted = new HashSet<ReferenceType>();
        this.setPattern();
        this.setPreconditionList();
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
        super(vmid);
        preconList.add(DeferrableRequestQueue.nullPrecondition);
        presetLocation = l;
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
    public synchronized Object resolveInternal(
			IDeferrableRequest.IResolutionContext rc) throws Exception {
		ReferenceType rt;
		VirtualMachine vm;

		if (isResolved())
			return REQUEST_RESOLVED;

		if (isCancelled())
			return REQUEST_CANCELLED;

		/*
		 * First precondition has been met - we might be able to set the
		 * breakpoint.
         * 
         * On VM connection do:
		 */
		if (rc.getPrecondition().getType().eventType() == IDeferrableRequest.VM_CONNECTION) {
			vm = this.getVMM().virtualMachine();

			if (pattern == null) {

				// TODO Loaded classes are fulfilled preconditions and should no
				// longer
				// be part of the advertised preconditions.

				/*
				 * We must ask the VM to notify us when our target class
				 * actually gets loaded.
				 */
			    String vmid = this.getVMM().getName();
			    DeferrableClassPrepareRequest cpr = new DeferrableClassPrepareRequest(
							vmid);
			    cpr.addClassFilter(classId);
			    IJavaNodeManager vmm = (IJavaNodeManager)vmmf.getNodeManager(vmid).getAdapter(IJavaNodeManager.class);
			    vmm.getDeferrableRequestQueue().addEagerlyResolve(cpr);
                
                assert loadRequest == null;
                loadRequest = wrapLoadRequest(cpr);

                rt = checkLoaded(classId);
                
                if(rt != null){
                    resolveWith(rt);
                    return OK;
                }
                
                return OK;
			}
			// If it's a pattern breakpoint, we'll have to try it against all
			// classes matching the pattern.
			else {
				/*
				 * We now check if there's a class prepare request for our
				 * pattern already.
                 * 
                 * This sucks. If I could specify that this breakpoint should receive
                 * events that pair to its request only, I wouldn't have to 
                 * worry about this at all.
                 * 
                 * Well, anyway, this is wrong. To preserve one-shot semantics (and keep
                 * broken with respect to multiple class loaders) I must remove the class
                 * prepare request (our) when the breakpoint is satisfied.
                 * 
				 */
//				for (ClassPrepareRequest cpr : (List<ClassPrepareRequest>) vm
//						.eventRequestManager().classPrepareRequests()) {
//					String filter = (String) cpr.getProperty(FILTER_PROP);
//					if (filter == null)
//						continue;
//					if (filter.equals(pattern)) {
//						inserted = cpr;
//						break;
//					}
//				}

                /**
                 * This should be a deferrable class prepare request,
                 * but deferrable class prepare requests are too buggy
                 * right now. Also, we know that the VM is online because
                 * this is a spawned request from a previous breakpoint 
                 * request.
                 */ 
			    ClassPrepareRequest cpr = vm.eventRequestManager().createClassPrepareRequest();
                cpr.setSuspendPolicy(PolicyManager.getInstance().getPolicy(ClassPrepareRequest.class));
				cpr.addClassFilter(pattern);
                cpr.enable();

                assert loadRequest == null;
                
                this.loadRequest = wrapLoadRequest(cpr);
                
                for (ReferenceType candidate : getAllLoaded()) {
                    Object request = resolveWith(candidate);
                    if (request != null)
                        return OK;
                }

				return OK;
			}

		}
		/* Second precondition has been met - we do a new attempt. 
         * 
         * When desired class has been loaded, do: */
		else if (rc.getPrecondition().getType().eventType() == IDeferrableRequest.CLASSLOADING) {
            rt = (ReferenceType)rc.getContext();
			/*
			 * Since with patterns we cannot check consistency by just trying to
			 * acquire the reference type (since it doesn't exist), we do it by
			 * matching the pattern against the class reported loaded.
			 */
			if (pattern != null) {
				String loaded = rt.name();
				if ((end && !loaded.endsWith(classId))
						|| (!end && !loaded.startsWith(classId))) {
					throw new InternalError("Reported class not expected - "
							+ loaded);
				}
			} 
			
			return this.resolveWith(rt);
		}

		/*
		 * This precondition is advertised by deferrable breakpoint requests
		 * that are created with the constructor that takes a location as a
		 * parameter.
		 */
		else if (rc.getPrecondition().getType().eventType() == IDeferrableRequest.NIL) {
			if (presetLocation == null)
				throw new InternalError(
						"Precondition NONE is only valid for Location breakpoints.");
			else
				return setForLocation(new Location[]{ presetLocation }, this.getVMM()
						.virtualMachine());
		} else {
			throw new InternalError("Unrecognized precondition.");
		}
	}
    
    private Object resolveWith(ReferenceType rt)
        throws Exception
    {
        if(isSetForRT(rt)) return REQUEST_RESOLVED; // Already set for this class. 
        
        VirtualMachine vm = this.getVMM().virtualMachine();
        Location [] breakLocation = null;
        // Two types of breakpoints.
        // 1 - Method breakpoints
        if(methodName != null){
            Method method = findMethod(rt);
            
            // If no line was specified, stops as soon as the method starts.
            if(line_no < 0){
                breakLocation = new Location[] { method.location() };
            }
            // Otherwise treats the line number as an offset into the method's
			// source code.
            else{
                List <Location> locs = method.allLineLocations();
                if(locs.size() > 0){
                		int baseLine = locs.get(0).lineNumber();
                		for(Location loc : locs){
                			int cLine = loc.lineNumber();
                			if((cLine + baseLine - 1) == baseLine){
                				breakLocation = new Location[] { loc };
                				break;
                			}
                		}
                }
            }
        }
        // 2 - Absolute line breakpoints.
        else{
            List <Location> locations = rt.locationsOfLine(line_no);
            if(locations.size() == 0){
                /* Well, the line doesn't exist. If we're a pattern breakpoint we do nothing. Otherwise it 
                 * means one of two things: 1) the user screwed up 2) we're an absolute line breakpoint meant
                 * to be set in an internal class. */
                if(pattern == null){
                    String vmid = this.getVMM().getName();

                    /** This request is our spawn and should die with us. */
                    DeferrableBreakpointRequest dbr = getSpawn(classId + "*", line_no);
                    
                    IJavaNodeManager vmm = (IJavaNodeManager)vmmf.getNodeManager(vmid).getAdapter(IJavaNodeManager.class);
                    vmm.getDeferrableRequestQueue().addEagerlyResolve(dbr);
                
                    /* Returns something just to get us out of the deferred queue (we'll be replaced by the 
                     *  new request we've just made).
                     */
                
                    return dbr;
                }else{
                    return null; /* Not this time, Jimbo. */
                }
            }
            
            breakLocation = new Location[locations.size()];
            breakLocation = locations.toArray(breakLocation);
        }
        
        // Finally, sets the breakpoint.
        breakpointSetForRT(rt);
        return setForLocation(breakLocation, vm);
    }
    
    private BreakpointRequest[] setForLocation(Location[] locs,
            VirtualMachine vm) {

        List<BreakpointRequest> requests = new ArrayList<BreakpointRequest>();

        for (Location loc : locs) {
            final BreakpointRequest br = vm.eventRequestManager()
                    .createBreakpointRequest(loc);
            PolicyManager pm = PolicyManager.getInstance();
            br.setSuspendPolicy(pm.getPolicy("request.breakpoint"));
            if (oneShot)
                br.addCountFilter(1);
            
            Iterator it = threadFilters.iterator();
            while (it.hasNext())
                br.addThreadFilter((ThreadReference) it.next());
            br.putProperty(DebuggerConstants.VMM_KEY, this.getVMM().getName());

            for (Object key : properties.keySet()) {
                br.putProperty(key, properties.get(key));
            }

            // Resolved.
            jdiRequests.add(new Cancelable() {
                public void cancel() throws Exception {
                    getVMM().virtualMachine().eventRequestManager()
                            .deleteEventRequest(br);
                }
            });
            
            /*
             * We are done. Notify whomever could be interested in this
             * resolution.
             */
            this.broadcastToListeners(br);

            /* Enables the request. */
            br.enable();
            requests.add(br);
        }

        BreakpointRequest[] brArray = new BreakpointRequest[requests.size()];

        return brArray;
    }
    
    private void breakpointSetForRT(ReferenceType rt){
        this.targeted.add(rt);
    }
    
    private boolean isSetForRT(ReferenceType rt){
        return this.targeted.contains(rt);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#getAllLoaded()
     */
    public List<ReferenceType> getAllLoaded(){
        VirtualMachine vm = this.getVMM().virtualMachine();
        List<ReferenceType> lst = new LinkedList<ReferenceType>();
        
        for(ReferenceType rt : (List<ReferenceType>)vm.allClasses()){
            if(end && rt.name().endsWith(classId)) lst.add(rt);
            else if(!end && rt.name().startsWith(classId)) lst.add(rt);
        }
        
        return lst;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#checkLoaded(java.lang.String)
     */
    public ReferenceType checkLoaded(String clsid){
    	VirtualMachine vm = this.getVMM().virtualMachine();
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

        IJavaNodeManager vmm = this.getVMM();
        VirtualMachine vm = vmm.virtualMachine();
        
        // Attempts to qualify this argument
        // First, tries to resolve directly
        List clist = vm.classesByName(normalized);
        if (clist.isEmpty()) {

            // Perhaps the class name is just incomplete?
            if (normalized.indexOf('.') == -1) {
                Iterator it = vm.allClasses().iterator();
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
    
    private DeferrableBreakpointRequest getSpawn(String pattern, int line_no) {
        DeferrableBreakpointRequest dbr = new DeferrableBreakpointRequest(this
                .getVMID(), new HashMap<Object, Object>(properties),
                new LinkedList(this.threadFilters), pattern, line_no);
        dbr.setOneShot(this.isOneShot());
        
        dbr.addResolutionListener(new IResolutionListener() {
            public void notifyResolution(IDeferrableRequest source,
                    Object byproduct) {
                broadcastToListeners(byproduct);
            }
        });
        
        /** Routes getProperty, setProperty, addThreadFilter, and isOneShot to the 
         * spawned breakpoint.
         */
        this.spawn = dbr;
        return dbr;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#addThreadFilter(java.lang.Integer)
     */
    public void addThreadFilter(Integer lt_uuid)
        throws NoSuchSymbolException {
        spawn.addThreadFilter0(lt_uuid);
    }

    public synchronized void addThreadFilter0(Integer lt_uuid)
			throws NoSuchSymbolException {
		checkResolved();
		IJavaNodeManager vmm = this.getVMM();
		/*
		 * First checks if there's a thread filter applied and if it's actually
		 * valid.
		 */
		IJavaThreadManager tm = vmm.getThreadManager();
		ThreadReference tr = tm.findThreadByUUID(lt_uuid);
		if (tr == null) {
			throw new NoSuchSymbolException("Can't find thread "
					+ ct.uuid2Dotted(lt_uuid.intValue()));
		}
		threadFilters.add(tr);
	}
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#putProperty(java.lang.Object, java.lang.Object)
     */
    public void putProperty(Object key, Object val){
        spawn.putProperty0(key, val);
    }

    public synchronized void putProperty0(Object key, Object val){
        checkResolved();
        properties.put(key, val);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#getProperty(java.lang.Object, java.lang.Object)
     */
    public Object getProperty(Object key, Object val){
        return spawn.getProperty0(key, val);
    }
    
    public synchronized Object getProperty0(Object key, Object val){
        checkResolved();
        return properties.get(key);
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#setOneShot(boolean)
     */
    public void setOneShot(boolean oneShot){
        spawn.setOneShot0(oneShot);
    }
    
    public synchronized void setOneShot0(boolean oneShot){
        checkResolved();
        this.oneShot = oneShot;
    }
    
    /* (non-Javadoc)
     * @see ddproto1.debugger.request.Internal#isOneShot()
     */
    public synchronized boolean isOneShot(){
        return spawn.isOneShot0();
    }
    
    public synchronized boolean isOneShot0(){
        return oneShot;
    }
    
    private void checkResolved(){
        if(isResolved())
            throw new IllegalStateException("Operation invalid on resolved event request.");
    }
    
    private boolean isResolved(){
    	return false;
    }
    
    public String toString() {
        StringBuffer stringRep = new StringBuffer();
        stringRep.append((this.methodName == null) ? "Absolute " : "Method ");
        stringRep.append("breakpoint in class ");
        stringRep.append(this.classId);
        stringRep.append(" at line ");
        stringRep.append((line_no == NO_LINE) ? "<no line>" : Integer
                .toString(line_no));
        if (pattern != null) {
            stringRep.append(" with pattern ");
            stringRep.append(pattern);
        }
        return stringRep.toString();
    }
    
    /**
     * Cancelling a DeferrableBreakpointRequest means:
     * 
     * - If the BreakpointRequest has already been placed, it will be
     *   deleted.
     * - If the BreakpointRequest hasn't yet been placed, it will not
     *   be placed after this method returns.
     *   
     *   Canceling cascades to all requests placed on behalf of this 
     *   breakpoint. 
     * 
     */
    protected synchronized void cancelInternal() throws Exception {
        boolean cancelFailed = false;
        
        // We must cancel the JDI request.
        Iterator <Cancelable> it = jdiRequests.iterator();
        while(it.hasNext()){
            Cancelable cancelable = it.next();
            try{
                cancelable.cancel();
                it.remove();
            }catch(Exception ex){
                cancelFailed = true;
            }
        }
        
        // Our deferrable class prepare request
        try{
            if(loadRequest != null) loadRequest.cancel();
        }catch(Exception ex){
            cancelFailed = true;
        }

        try{
            if(spawn != this)
                this.getVMM().getDeferrableRequestQueue().removeRequest(spawn);
        }catch(Exception ex){
            cancelFailed = true;
        }
        
        if(cancelFailed)
            throw new TargetRequestFailedException(); 
    }
    
    protected Cancelable wrapLoadRequest(final ClassPrepareRequest request){
        return new Cancelable(){
            public void cancel() throws Exception {
                getVMM().virtualMachine().eventRequestManager().deleteEventRequest(request);
            }
        };
    }
    
    protected Cancelable wrapLoadRequest(final DeferrableClassPrepareRequest request){
        return new Cancelable(){
            public void cancel() throws Exception {
                getVMM().getDeferrableRequestQueue().removeRequest(request);
            }
        };
    }

    private interface Cancelable{
        public void cancel() throws Exception;
    }
}
