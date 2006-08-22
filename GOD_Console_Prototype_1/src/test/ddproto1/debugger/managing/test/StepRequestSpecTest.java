/*
 * Created on 21/08/2006
 * 
 * file: StepRequestSpecTest.java
 */
package ddproto1.debugger.managing.test;

import junit.framework.TestCase;
import ddproto1.debugger.managing.StepRequestSpec;

public class StepRequestSpecTest extends TestCase{
    public void testEnableDisable(){
        StepRequestSpec srSpec = StepRequestSpec.quiet();
        srSpec = srSpec.enable(StepRequestSpec.GENERATE_STEP_END | 
                StepRequestSpec.GENERATE_STEP_START);
        
        /** Checks if enablement affects other properties */
        assertTrue(srSpec.generateStepEnd());
        assertTrue(srSpec.generateStepStart());
        assertFalse(srSpec.generateParentStepEnd());
        assertFalse(srSpec.generateParentStepStart());
        assertFalse(srSpec.updateParentAtEnd());
        assertFalse(srSpec.updateParentAtStart());
        
        srSpec = srSpec.enable(StepRequestSpec.UPDATE_PARENT_AT_END);
        assertTrue(srSpec.updateParentAtEnd());
        
        /** Checks if disablement affects other properties */
        srSpec = srSpec.disable(StepRequestSpec.UPDATE_PARENT_AT_END
                | StepRequestSpec.GENERATE_STEP_START);
        assertFalse(srSpec.updateParentAtEnd());
        
        assertTrue(srSpec.generateStepEnd());
        assertFalse(srSpec.generateStepStart());
        assertFalse(srSpec.generateParentStepEnd());
        assertFalse(srSpec.generateParentStepStart());
        assertFalse(srSpec.updateParentAtEnd());
        assertFalse(srSpec.updateParentAtStart());
    }
}
