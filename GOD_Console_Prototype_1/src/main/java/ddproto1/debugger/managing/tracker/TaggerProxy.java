/*
 * Created on Sep 28, 2004
 * 
 * file: RemoteThreadUtility.java
 */
package ddproto1.debugger.managing.tracker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

import ddproto1.commons.DebuggerConstants;
import ddproto1.exception.AmbiguousSymbolException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.util.MessageHandler;

/**
 * Potpourri of utility methods used by event processors within the
 * ddproto1.debugger.managing.tracker. Acts as a sort of proxy
 * to the tagger class. <BR>
 * <BR>
 * This class exists mainly becaus invoking remote methods in the debuggee 
 * is a real pain. A Java parser that converts expressions into debugger 
 * calls would be really nice (actually there is one but it is proprietary). 
 * The REAAAAALLY boring part is checking whether the symbol has resolved 
 * unanbiguously or not.<BR>
 * <BR>
 * Doesn't maintain state because it might be used by multiple machines
 * concurrently.<BR> 
 * 
 * @author giuliano
 *
 */
public class TaggerProxy {
    private static TaggerProxy instance;
    private static MessageHandler mh = MessageHandler.getInstance();
    
    public synchronized static TaggerProxy getInstance(){
        return (instance == null)?(instance = new TaggerProxy()):instance;
    }
    
    private TaggerProxy(){  }
    
    public ClassType getClass(VirtualMachine vm, String classname)
    	throws NoSuchSymbolException, AmbiguousSymbolException
    {
        ClassType taggerClass = null;
        
        List types = vm.classesByName(classname);
        taggerClass = (ClassType)getUnique(types, "Error - no tagger class " +
        		"could be found! (Has your application been loaded " +
        		"using the debugger application launcher?) ", classname);
        
        return taggerClass;
    }
    
    public ObjectReference getInstance(ClassType rt, ThreadReference executor)
            throws NoSuchSymbolException, ClassNotLoadedException,
            IncompatibleThreadStateException, InvalidTypeException,
            InvocationException, AmbiguousSymbolException {

        List minstance = rt.methodsByName(DebuggerConstants.INSTANCE_GETTER);
        
        Method getInstance = (Method) getUnique(minstance, "Tagger class "
                + rt.name() + " does not support the "
                + "required interface. Couldn't find method "
                + DebuggerConstants.INSTANCE_GETTER,
                DebuggerConstants.INSTANCE_GETTER);
        
        ObjectReference tagger;
        
        /* Gets the instance */
        tagger = (ObjectReference) rt.invokeMethod(executor,
                getInstance, new ArrayList(), ObjectReference.INVOKE_SINGLE_THREADED);
        
        return tagger;
    }
    
    public Method getMethod(ClassType rt, String method)
    	throws NoSuchSymbolException, AmbiguousSymbolException
    {
        List minstance = rt.methodsByName(method);

        Method mt = (Method) getUnique(minstance, "Tagger class "
                + rt.name() + " does not support the "
                + "required interface. Couldn't find method " + method,
                method);
        
        return mt;
    }
    
    public IntegerValue getRemoteUUID(ThreadReference te, ClassType taggerClass) throws Exception {

        ObjectReference t_instance = this.getInstance(taggerClass, te);

        Method getUUID = this.getMethod(taggerClass,
                DebuggerConstants.UUID_GETTER);
        IntegerValue uuid = (IntegerValue) t_instance.invokeMethod(te, getUUID,
                new ArrayList(), ClassType.INVOKE_SINGLE_THREADED);

        return uuid;
    }

    private IntegerValue partOf(ThreadReference te) throws Exception {
        return null;
    }

    private Object getUnique(List l, String none, String who)
            throws NoSuchSymbolException, AmbiguousSymbolException {
        if (l.size() == 0) {
            throw new NoSuchSymbolException(none);
        }

        Iterator it = l.iterator();
        Object unique = it.next();

        if (l.size() > 1) {
            throw new AmbiguousSymbolException(
                    "Error - "
                            + who
                            + " resolved to more than one "
                            + " reference. This debugging session will be using "
                            + unique
                            + " as representative."
                            + " Though this is not a fatal error, further errors may occur.");
        }

        return unique;
    }
}
