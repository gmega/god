/*
 * Created on 21/08/2006
 * 
 * file: StepRequestSpec.java
 */
package ddproto1.debugger.managing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a specification for a java step request. Allows the 
 * specification of custom policies for generating events, as well 
 * as attributes to be passed on to the step request itself. 
 * 
 * For internal use.
 * 
 * @author giuliano
 *
 */
public class StepRequestSpec {
    public static final int GENERATE_STEP_START = 1;
    public static final int GENERATE_STEP_END = 2;
    public static final int PARENT_GENERATE_STEP_START   = 4;
    public static final int PARENT_GENERATE_STEP_END   = 8;
    public static final int UPDATE_PARENT_AT_START = 16;
    public static final int UPDATE_PARENT_AT_END = 32;
    public static final int RESUME_ON_START = 64;
    
    public static final int FULL_NOTIFICATION = GENERATE_STEP_START | 
                                        GENERATE_STEP_END |
                                        PARENT_GENERATE_STEP_START |
                                        PARENT_GENERATE_STEP_END |
                                        UPDATE_PARENT_AT_START |
                                        UPDATE_PARENT_AT_END |
                                        RESUME_ON_START;
    
    public static final int QUIET = RESUME_ON_START;
    
    public static final int QUIET_DELAYED = 0;
    
    private volatile Map<Object, Object> properties;
    
    private volatile Map<Object, Object> unmodWrapper;
    
    private volatile int fStepFlags;
    
    public StepRequestSpec(int stepFlags){
        this(stepFlags, new HashMap<Object, Object>());
    }
    
    private StepRequestSpec(int stepFlags, Map<Object, Object> attMap){
        fStepFlags = stepFlags;
        properties = Collections.synchronizedMap(attMap);
        unmodWrapper = Collections.unmodifiableMap(attMap);
    }
    
    public StepRequestSpec enable(int stepFlags){
        return copyWith(fStepFlags | stepFlags);
    }
    
    public StepRequestSpec disable(int stepFlags){
        return copyWith((FULL_NOTIFICATION^stepFlags)&fStepFlags);
    }
    
    private StepRequestSpec copyWith(int stepFlags){
        return new StepRequestSpec(stepFlags, 
                new HashMap<Object, Object>(getPropertyMap()));
    }
    
    public boolean generateStepStart(){
        return (fStepFlags & GENERATE_STEP_START) != 0;
    }
        
    public boolean generateStepEnd(){
        return (fStepFlags & GENERATE_STEP_END) != 0;
    }
    
    public boolean generateParentStepStart(){
        return (fStepFlags & PARENT_GENERATE_STEP_START) != 0;
    }
    
    public boolean generateParentStepEnd(){
        return (fStepFlags & PARENT_GENERATE_STEP_END) != 0;
    }

    public boolean updateParentAtStart(){
        return (fStepFlags & UPDATE_PARENT_AT_START) != 0;
    }

    public boolean updateParentAtEnd(){
        return (fStepFlags & UPDATE_PARENT_AT_END) != 0;
    }
    
    public boolean shouldResume(){
        return (fStepFlags & RESUME_ON_START) != 0;
    }
    
    public void setProperty(Object key, Object val){
        properties.put(key, val);
    }
    
    public Object removeProperty(Object key){
        return properties.remove(key);
    }
    
    public Map <Object, Object> getPropertyMap(){
        return unmodWrapper;
    }
    
    public static StepRequestSpec fullNotification(){
        return new StepRequestSpec(FULL_NOTIFICATION);
    }
    
    public static StepRequestSpec quiet(){
        return new StepRequestSpec(QUIET);
    }
    
    public static StepRequestSpec quietDelayed(){
        return new StepRequestSpec(QUIET_DELAYED);
    }
}
