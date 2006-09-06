/*
 * Created on 14/08/2006
 * 
 * file: DistributedThreadFrameMappingTest.java
 */
package ddproto1.debugger.managing.tracker;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;

import ddproto1.debugger.managing.IThreadManager;
import ddproto1.debugger.managing.tracker.ILocalThread;
import ddproto1.debugger.managing.tracker.VirtualStackframe;

public class DistributedThreadFrameMappingTest extends TestCase {
    
    private static int ids = 0;
    private static int frameId = 0;

    public void testFrameMapping()
        throws Exception
    {
        IDebugTarget target = generateDebugTarget();
        IThreadManager tManager = generateThreadManager();
        
        ILocalThread t1 = generateThread(5);
        ILocalThread t2 = generateThread(3);
        ILocalThread t3 = generateThread(2);
        
        EasyMock.replay(target, tManager, t1, t2, t3);
        
        // This virtual frame contributes with four real frames.
        VirtualStackframe vsf1 = generateStackframe(null, "outCall", 2, -1, t1, target);
        
        // This one, with three real frames.
        VirtualStackframe vsf2 = generateStackframe(null, "outCall", 1, 3, t2, target);
        
        // And this one, with one. 
        VirtualStackframe vsf3 = generateStackframe(null, "outCall", -1, 1, t3, target);
        
        DistributedThread dThread = new DistributedThread(vsf3, null, target);
        
        dThread.lock();
        dThread.virtualStack().pushFrame(vsf2);
        dThread.virtualStack().pushFrame(vsf1);
        dThread.unlock();

        int [] expected  = {0, 1, 2, 3, 5, 6, 7, 9};
        IStackFrame [] realFrames = dThread.getStackFrames();
        
        assertTrue(realFrames.length == expected.length);
        
        for(int i = 0; i < expected.length; i++)
            assertTrue(expected[i] == realFrames[i].getLineNumber());
        
        EasyMock.verify(t1, t2, t3);
    }
    
    private VirtualStackframe generateStackframe(String in, String out, int callbase, int calltop, 
            ILocalThread ref, IDebugTarget parent){
        VirtualStackframe vsf = new VirtualStackframe(in, out, ref, parent);
        if(callbase > 0) vsf.setCallBase(callbase);
        if(calltop > 0) vsf.setCallTop(calltop);
        return vsf;
    }
    
    private ILocalThread generateThread(int nFrames)
        throws Exception
    {
        ILocalThread tr = 
            EasyMock.createMock(ILocalThread.class);
        
        IStackFrame [] frames = new IStackFrame[nFrames];
        for(int i = 0; i < frames.length; i++){
            frames[i] = EasyMock.createMock(IStackFrame.class);
            EasyMock.expect(frames[i].getLineNumber()).andReturn(frameId++).anyTimes();
            EasyMock.replay(frames[i]);
        }
        EasyMock.expect(tr.getGUID()).andReturn(new Integer(ids++)).anyTimes();
        EasyMock.expect(tr.setParentDT(EasyMock.isA(DistributedThread.class))).andReturn(true).atLeastOnce();
        EasyMock.expect(tr.getStackFrames()).andReturn(frames).atLeastOnce();
        
        return tr;
    }
    
    private IDebugTarget generateDebugTarget(){
        return EasyMock.createMock(IDebugTarget.class);
    }
    
    private IThreadManager generateThreadManager(){
        return EasyMock.createMock(IThreadManager.class);
    }
}
