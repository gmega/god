/*
 * Created on 11/09/2006
 * 
 * file: TaggerTest.java
 */
package ddproto1.localagent.test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ddproto1.localagent.Tagger;
import ddproto1.util.MessageHandler;

import junit.framework.TestCase;

public class TaggerTest extends TestCase {
    
    private static final int EXPECTED_MAX_RUNTIME = 10000;
    private static final int RUN_THREADS = 100;
    private static final byte GID = (byte)255;
    private static final Logger logger = MessageHandler.getInstance()
            .getLogger(TaggerTest.class);
    
    private final Random rnd = 
        new Random(System.currentTimeMillis());
        
    private final CyclicBarrier fInternalBarrier =
        new CyclicBarrier(RUN_THREADS);
    
    private final CyclicBarrier fTerminationBarrier =
        new CyclicBarrier(RUN_THREADS + 1);

    
    private final Set<Integer> idSet =
        Collections.synchronizedSet(new HashSet<Integer>());
    
    private volatile boolean failed = false;

    public void testSetUp(){
        Tagger.getInstance().setGID((byte)255);
    }
    
    public void testIDAssignment()
        throws Exception {
        runTest(IDAssignmentThread.class);
    }
    
    public void testSteppingAssignment()
        throws Exception {
        runTest(StepAssignmentThread.class);
    }
    
    public void testPartOfTracking()
        throws Exception
    {
        runTest(PartOfThread.class);
    }
    
    private void failed(){
        failed = true;
    }
    
    private class IDAssignmentThread implements Runnable{
        
        public void run() {
            try{
                Tagger tagger = Tagger.getInstance();
                fInternalBarrier.await();
                tagger.tagCurrent();
                fInternalBarrier.await();
                assertTrue(tagger.isCurrentTagged());
                int myTag = tagger.currentTag();
                if(idSet.contains(myTag))
                    failed();
                assertTrue(((byte)(myTag >> 24)) == GID);
                idSet.add(myTag);
                fTerminationBarrier.await();
            }catch(Exception ex){
                logger.error(ex);
                failed();
            }
        }
    }
    
    private class StepAssignmentThread implements Runnable{

        private volatile boolean fSetStepping;
        
        public StepAssignmentThread(){
            fSetStepping = rnd.nextBoolean();
        }
        
        public void run() {
            try{
                Tagger tagger = Tagger.getInstance();
                fInternalBarrier.await();
                if(fSetStepping) tagger.setStepping();
                fInternalBarrier.await();
                if(tagger.isStepping() != fSetStepping)
                    failed();
                fTerminationBarrier.await();
            }catch(Exception ex){
                logger.error(ex);
                failed();
            }
        }
        
    }
    
    private class PartOfThread implements Runnable{

        private int fParentDT;
        
        private PartOfThread(){
            fParentDT = rnd.nextInt();
        }
        
        public void run() {
            try {
                Tagger tagger = Tagger.getInstance();
                fInternalBarrier.await();
                tagger.tagCurrent();
                fInternalBarrier.await();
                tagger.makePartOf(fParentDT);
                fInternalBarrier.await();
                assertTrue(tagger.partOf() == fParentDT);
                tagger.unmakePartOf();
                assertTrue(tagger.partOf() == tagger.currentTag());
                fTerminationBarrier.await();
            } catch (Exception ex) {
                logger.error(ex);
                failed();
            }
        }
        
    }
    
    private void runTest(Class c)
        throws Exception
    {
        Constructor cons = c.getDeclaredConstructor(new Class [] { TaggerTest.class });
        for(int i = 0; i < RUN_THREADS; i++)
            new Thread((Runnable)cons.newInstance(new Object [] { this })).start();
        
        fTerminationBarrier.await(EXPECTED_MAX_RUNTIME, TimeUnit.MILLISECONDS);
        assertFalse(failed);
    }
}
